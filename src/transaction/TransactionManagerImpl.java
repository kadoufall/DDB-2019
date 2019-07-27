package transaction;

import transaction.exceptions.InvalidTransactionException;
import transaction.exceptions.TransactionAbortedException;

import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the TM
 */

public class TransactionManagerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {
    private final static String TM_TRANSACTION_NUM_LOG_FILENAME = "data/wc_xidNum.log";
    private final static String TM_TRANSACTION_LOG_FILENAME = "data/wc_xids.log";
    private final static String TM_TRANSACTION_RMs_LOG_FILENAME = "data/wc_xidRMs.log";

    private String dieTime;
    private Integer xidNum;
    private Map<Integer, TransactionStatus> xids;
    private Map<Integer, Set<ResourceManager>> xidRMs;

    public TransactionManagerImpl() throws RemoteException {
        this.dieTime = "NoDie";
        this.xidNum = 1;
        this.xids = new HashMap<>();
        this.xidRMs = new HashMap<>();

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
            this.xids = (Map<Integer, TransactionStatus>) cacheXids;
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
                this.xids.put(curXid, TransactionStatus.NEW);
                this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
            }

            synchronized (this.xidRMs) {
                this.xidRMs.put(curXid, new HashSet<>());
            }

            return curXid;
        }

    }

    @Override
    public boolean commit(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        if (!this.xids.containsKey(xid)) {
            throw new InvalidTransactionException(xid, "TM commit");
        }

        System.out.println(xid);

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
                e.printStackTrace();
                this.abort(xid);
                throw new TransactionAbortedException(xid, "commit prepare");
            }
        }

        synchronized (this.xids) {
            xids.put(xid, TransactionStatus.PREPARED);
            this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
        }

        // the TM fails after it has received
        // "prepared" messages from all RMs, but before it can log "committed"
        if (this.dieTime.equals("BeforeCommit")) {
            this.dieNow();
        }

        // log "committed"
        synchronized (this.xids) {
            xids.put(xid, TransactionStatus.COMMITTED);
            this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
        }

        // the TM fails right after it logs "committed"
        if (this.dieTime.equals("AfterCommit")) {
            this.dieNow();
        }

        Set<ResourceManager> committedRMs = new HashSet<>();
        for (ResourceManager resourceManager : resourceManagers) {
            try {
                resourceManager.commit(xid);
                committedRMs.add(resourceManager);
            } catch (Exception e) {
                // occur when RM die BeforeCommit
                // when RM re-enlist, commit xid
            }
        }

        if (committedRMs.size() == resourceManagers.size()) {
            synchronized (this.xidRMs) {
                this.xidRMs.remove(xid);
            }

            synchronized (this.xids) {
                this.xids.remove(xid);
                this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
            }
        } else {
            // some RMs die
            synchronized (this.xidRMs) {
                resourceManagers.removeAll(committedRMs);
                this.xidRMs.put(xid, resourceManagers);
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

        for (ResourceManager resourceManager : resourceManagers) {
            resourceManager.abort(xid);
        }

        synchronized (this.xidRMs) {
            this.xidRMs.remove(xid);
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

        if (this.xids.get(xid) == TransactionStatus.COMMITTED) {
            rm.commit(xid);
            Set<ResourceManager> temp = this.xidRMs.get(xid);
            ResourceManager find = null;
            for (ResourceManager r : temp) {
                if (r.getID().equals(rm.getID())) {
                    find = r;
                    break;
                }
            }

            if (find != null) {
                temp.remove(find);
                synchronized (this.xidRMs) {
                    if (temp.size() == 0) {
                        this.xidRMs.remove(xid);
                    } else {
                        this.xidRMs.put(xid, temp);
                    }
                }
                synchronized (this.xids) {
                    if (temp.size() == 0) {
                        this.xids.remove(xid);
                        this.storeToFile(TM_TRANSACTION_LOG_FILENAME, this.xids);
                    }
                }
            }
            return;
        }

        synchronized (this.xidRMs) {
            Set<ResourceManager> resourceManagers = this.xidRMs.get(xid);
            if (resourceManagers == null) {
                resourceManagers = new HashSet<>();
            }

            resourceManagers.add(rm);

            this.xidRMs.put(xid, resourceManagers);
        }
    }

    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());

        String rmiPort = System.getProperty("rmiPort");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            TransactionManagerImpl obj = new TransactionManagerImpl();
            Naming.rebind(rmiPort + TransactionManager.RMIName, obj);

            // TODO

            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
    }


}
