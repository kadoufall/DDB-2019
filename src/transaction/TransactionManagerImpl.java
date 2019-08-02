package transaction;

import transaction.exceptions.InvalidTransactionException;
import transaction.exceptions.TransactionAbortedException;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the TM
 */

public class TransactionManagerImpl extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {
    // TM xidsNum and xids cache data
    private final static String TM_TRANSACTION_NUM_LOG_FILENAME = "data/tm_xidNum.log";
    private final static String TM_TRANSACTION_LOG_FILENAME = "data/tm_xids.log";
    private final static String TM_TRANSACTION_RMs_LOG_FILENAME = "data/tm_xidRMs.log";

    private String dieTime;

    // monotonically increasing xid
    private Integer xidNum;

    // xid -> TransactionStatus
    private Map<Integer, String> xids;

    // xid -> all related RMs (added by enlist)
    private Map<Integer, Set<ResourceManager>> xidRMs;

    public TransactionManagerImpl() throws RemoteException {
        this.dieTime = "NoDie";
        this.xidNum = 1;
        this.xids = new HashMap<>();
        this.xidRMs = new HashMap<>();

        // if TM die, restart and recover from cache data
        this.recoverFromFile();
    }

    public void setDieTime(String dieTime) {
        this.dieTime = dieTime;
    }

    private void recoverFromFile() {
        // load xidNum from file
        Object cacheXidNum = this.loadFromFile(TM_TRANSACTION_NUM_LOG_FILENAME);
        if (cacheXidNum != null) {
            this.xidNum = (Integer) cacheXidNum;
        }

        // load xids from file
        Object cacheXids = this.loadFromFile(TM_TRANSACTION_LOG_FILENAME);
        if (cacheXids != null) {
            this.xids = (Map<Integer, String>) cacheXids;
        }

        // load xids from file
        Object cacheXidRMs = this.loadFromFile(TM_TRANSACTION_RMs_LOG_FILENAME);
        if (cacheXidRMs != null) {
            this.xidRMs = (Map<Integer, Set<ResourceManager>>) cacheXidRMs;
        }

        // if TM die, all related xids must be aborted
        // except for those who have committed: dieTMAfterCommit
        Set<Integer> keys = this.xids.keySet();
        for (Integer key : keys) {
            if (!this.xids.get(key).equals(TransactionManager.COMMITTED)) {
                this.xids.put(key, TransactionManager.ABORTED);
            }
        }

    }

    private Object loadFromFile(String filePath) {
        File file = new File(filePath);
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(new FileInputStream(file));
            return oin.readObject();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (oin != null)
                    oin.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void storeToFile(String filePath, Object obj) {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(new FileOutputStream(file));
            oout.writeObject(obj);
            oout.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (oout != null)
                    oout.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public int start() throws RemoteException {
        synchronized (this.xidNum) {
            Integer curXid = this.xidNum++;
            this.storeToFile(TM_TRANSACTION_NUM_LOG_FILENAME, this.xidNum);

            synchronized (this.xids) {
                this.xids.put(curXid, TransactionManager.NEW);
                this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
            }

            synchronized (this.xidRMs) {
                this.xidRMs.put(curXid, new HashSet<>());
                this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
            }

            return curXid;
        }
    }

    @Override
    public boolean commit(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        if (!this.xids.containsKey(xid)) {
            throw new TransactionAbortedException(xid, "TM commit");
        }

        // prepare
        Set<ResourceManager> resourceManagers = this.xidRMs.get(xid);
        for (ResourceManager resourceManager : resourceManagers) {
            try {
                boolean prepared = resourceManager.prepare(xid);
                if (!prepared) {
                    this.abort(xid);
                    throw new TransactionAbortedException(xid, "commit prepare");
                }
            } catch (Exception e) {
                // occur when RM die, AfterPrepare, BeforePrepare
                // e.printStackTrace();
                this.abort(xid);
                throw new TransactionAbortedException(xid, "commit prepare");
            }
        }

        // write the prepare log to disk and mark xid PREPARED
        synchronized (this.xids) {
            xids.put(xid, TransactionManager.PREPARED);
            this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
        }

        // the TM fails after it has received
        // "prepared" messages from all RMs, but before it can log "committed"
        // if this occur, all rm call enlist later when TM restart,
        // and enlist will mark xid ABORTED
        if (this.dieTime.equals("BeforeCommit")) {
            this.dieNow();
        }

        // write the prepare log to disk and mark xid COMMITTED
        synchronized (this.xids) {
            xids.put(xid, TransactionManager.COMMITTED);
            this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
        }

        // the TM fails right after it logs "committed"
        // if this occur, recoverFromFile will recover the xid
        // enlist commit RM
        if (this.dieTime.equals("AfterCommit")) {
            this.dieNow();
        }

        Set<ResourceManager> committedRMs = new HashSet<>();
        for (ResourceManager resourceManager : resourceManagers) {
            try {
                resourceManager.commit(xid);
                committedRMs.add(resourceManager);
            } catch (Exception e) {
//                e.printStackTrace();
                // FdieRMBeforeCommit
            }
        }

        if (committedRMs.size() == resourceManagers.size()) {
            synchronized (this.xidRMs) {
                this.xidRMs.remove(xid);
                this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
            }

            synchronized (this.xids) {
                this.xids.remove(xid);
                this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
            }
        } else {
            // FdieRMBeforeCommit
            synchronized (this.xidRMs) {
                resourceManagers.removeAll(committedRMs);
                this.xidRMs.put(xid, resourceManagers);
                this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
            }
        }

        return true;
    }

    @Override
    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        if (!this.xids.containsKey(xid)) {
            throw new InvalidTransactionException(xid, "TM abort");
        }

        Set<ResourceManager> resourceManagers = this.xidRMs.get(xid);
        Set<ResourceManager> abortedRMs = new HashSet<>();
        for (ResourceManager resourceManager : resourceManagers) {
            try {
                resourceManager.abort(xid);
                abortedRMs.add(resourceManager);
            } catch (Exception e) {
//                e.printStackTrace();

                // FdieRMBeforeAbort
            }
        }

        synchronized (this.xidRMs) {
            this.xidRMs.remove(xid);
            this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
        }

        synchronized (this.xids) {
            this.xids.remove(xid);
            this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
        }

    }

    public boolean dieNow() throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }

    public void ping() throws RemoteException {
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException, InvalidTransactionException {
        if (!this.xids.containsKey(xid)) {
            rm.abort(xid);
            return;
        }


        synchronized (this.xids) {
            String resourceStatus = this.xids.get(xid);

            // if one of the other RMs with the same xid die, then all RM must abort the xid
            // this will occur when other xid has already been marked ABORTED

            // if TM die and restart, all RM will abort
            if (resourceStatus.equals(TransactionManager.ABORTED)) {
                rm.abort(xid);

                synchronized (this.xidRMs) {
                    Set<ResourceManager> temp = this.xidRMs.get(xid);
                    ResourceManager randomRemove = temp.iterator().next();
                    temp.remove(randomRemove);
                    if (temp.size() > 0) {
                        this.xidRMs.put(xid, temp);
                        this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
                    } else {
                        this.xidRMs.remove(xid);
                        this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);

                        this.xids.remove(xid);
                        this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
                    }
                }

                return;
            }

            // dieRMBeforeCommit, dieTMAfterCommit
            if (resourceStatus.equals(TransactionManager.COMMITTED)) {
                rm.commit(xid);

                synchronized (this.xidRMs) {
                    Set<ResourceManager> temp = this.xidRMs.get(xid);
                    ResourceManager randomRemove = temp.iterator().next();
                    temp.remove(randomRemove);
                    if (temp.size() == 0) {
                        this.xidRMs.remove(xid);
                        this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);

                        this.xids.remove(xid);
                        this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
                    } else {
                        this.xidRMs.put(xid, temp);
                        this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
                    }
                }

                return;
            }

            synchronized (this.xidRMs) {
                Set<ResourceManager> temp = this.xidRMs.get(xid);
                ResourceManager findSameRMId = null;
                boolean abort = false;
                for (ResourceManager r : temp) {
                    try {
                        if (r.getID().equals(rm.getID())) {
                            findSameRMId = r;
                        }
                    } catch (Exception e) {
                        // if some RM die, then r.getID() will cause an exception
                        // dieRM, dieRMAfterEnlist,
                        abort = true;
                        break;
                    }
                }

                if (abort) {
                    rm.abort(xid);

                    ResourceManager randomRemove = temp.iterator().next();
                    temp.remove(randomRemove);
                    if (temp.size() > 0) {
                        this.xidRMs.put(xid, temp);
                        this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);

                        // for dieRM, dieRMAfterEnlist
                        this.xids.put(xid, TransactionManager.ABORTED);
                        this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
                    } else {
                        this.xidRMs.remove(xid);
                        this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);

                        this.xids.remove(xid);
                        this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
                    }

                    return;
                }


                // new enlist
                if (findSameRMId == null) {
                    temp.add(rm);
                    this.xidRMs.put(xid, temp);
                    this.storeToFile(TM_TRANSACTION_RMs_LOG_FILENAME, this.xidRMs);
                    return;
                }

                // same rm re-enlist, do nothing

            }
        }
    }

    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());

        String rmiPort = System.getProperty("rmiPort");
        rmiPort = Utils.getOriginRmiport(rmiPort);

        try {
            TransactionManagerImpl obj = new TransactionManagerImpl();
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
            registry.rebind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
    }


}
