package transaction;

import transaction.exceptions.InvalidTransactionException;
import transaction.exceptions.TransactionAbortedException;
import transaction.models.ResourceItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.*;
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
    private final static String XID_NUM_FILENAME = "xidNum.log";
    private final static String XIDs_FILENAME = "xidNum.log";

    private String dieTime;

    private Integer xidNum;

    private Set<Integer> xids;

    private Map<Integer, Set<ResourceManager>> xidRMs;


    public TransactionManagerImpl() throws RemoteException {
        dieTime = "NoDie";

        xidNum = 1;
        xids = new HashSet<>();

        // recover after TM restart
//        this.recover();

    }

    public void setDieTime(String dieTime) {
        this.dieTime = dieTime;
    }

    private void recover() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
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

    @Override
    public int start() throws RemoteException {

        synchronized (this.xidNum) {
            int curXid = this.xidNum;
            this.xidNum++;

            synchronized (this.xids) {
                this.xids.add(curXid);
            }


            return curXid;
        }

    }

    @Override
    public boolean commit(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "TM commit");
        }

        Set<ResourceManager> resourceManagers = this.xidRMs.get(xid);

        for (ResourceManager resourceManager : resourceManagers) {
            boolean prepared = resourceManager.prepare(xid);
            if (!prepared) {
                this.abort(xid);
                throw new TransactionAbortedException(xid, "");
            }
        }

        for (ResourceManager resourceManager : resourceManagers) {
            resourceManager.commit(xid);
        }

        synchronized (this.xidRMs) {
            this.xidRMs.remove(xid);
        }

        synchronized (this.xids) {
            this.xids.remove(xid);
        }

        return false;
    }

    @Override
    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
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
        }

    }

    public boolean dieNow() throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }

    public void ping() throws RemoteException {
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException {
//        if (!this.xids.contains(xid)) {
//
//        }

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
