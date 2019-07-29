package transaction;

import lockmgr.DeadlockException;
import lockmgr.LockManager;
import transaction.exceptions.InvalidIndexException;
import transaction.exceptions.InvalidTransactionException;
import transaction.exceptions.TransactionManagerUnaccessibleException;
import transaction.models.ResourceItem;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.*;

/**
 * Resource Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the RM
 */

public class ResourceManagerImpl extends java.rmi.server.UnicastRemoteObject implements ResourceManager {
    private final static String RM_TRANSACTION_LOG_FILENAME = "transactions.log";

    private TransactionManager tm = null;

    private LockManager lm = new LockManager();

    // RMs
    private String myRMIName = null; // Used to distinguish this RM from other

    private HashSet xids = new HashSet();     // all transactions

    // xid -> xidtables, xidtables: tableName -> RMTable
    // RMTable, each RMTable is related to a data table,
    // and each line is a contains entries with a query, update, insert or delete
    private Hashtable tables = new Hashtable();

    private String dieTime;

    //  test usage
    public ResourceManagerImpl() throws RemoteException {
    }

    public ResourceManagerImpl(String rmiName) throws RemoteException {
        myRMIName = rmiName;
        dieTime = "NoDie";

        recover();

        while (!reconnect()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        new Thread(() -> {
            while (true) {
                try {
                    if (tm != null) {
                        tm.ping();
                    }
                } catch (Exception e) {
                    System.out.println("tm is null");
                    e.printStackTrace();
                    tm = null;
                }

                if (tm == null) {
                    reconnect();
                    System.out.println("reconnect tm!");

                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public TransactionManager getTransactionManager() throws TransactionManagerUnaccessibleException {
        if (tm != null) {
            try {
                tm.ping();
            } catch (RemoteException e) {
                tm = null;
            }
        }
        if (tm == null) {
            if (!reconnect())
                tm = null;
        }
        if (tm == null)
            throw new TransactionManagerUnaccessibleException();
        else
            return tm;
    }

    public void setTransactionManager(TransactionManager tm) {
        this.tm = tm;
    }

    protected LockManager getLockManager() {
        return lm;
    }

    public Set getTransactions() {
        return xids;
    }

    public Collection getUpdatedRows(int xid, String tablename) {
        RMTable table = getTable(xid, tablename);
        return new ArrayList(table.table.values());
    }

    public Collection getUpdatedRows(String tablename) {
        RMTable table = getTable(tablename);
        return new ArrayList(table.table.values());
    }

    public void setDieTime(String time) throws RemoteException {
        dieTime = time;
        System.out.println("Die time set to : " + time);
    }

    public String getID() throws RemoteException {
        return myRMIName;
    }

    public void ping() {
    }

    public void recover() {
        HashSet t_xids = loadTransactionLogs();
        if (t_xids != null)
            xids = t_xids;

        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        File[] datas = dataDir.listFiles();
        //main table
        for (int i = 0; i < datas.length; i++) {
            if (datas[i].isDirectory()) {
                continue;
            }
            if (datas[i].getName().endsWith(".log")) {
                continue;
            }

            // get the four main tables: Flight, Car, Customer, Hotel
            getTable(datas[i].getName());
        }

        //xtable
        for (int i = 0; i < datas.length; i++) {
            if (!datas[i].isDirectory())
                continue;
            File[] xdatas = datas[i].listFiles();
            int xid = Integer.parseInt(datas[i].getName());
            if (!xids.contains(xid)) {
                //this should never happen;
                throw new RuntimeException("ERROR: UNEXPECTED XID");
            }
            for (int j = 0; j < xdatas.length; j++) {
                RMTable xtable = getTable(xid, xdatas[j].getName());
                try {
                    xtable.relockAll();
                } catch (DeadlockException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean reconnect() {
        String rmiPort = System.getProperty("rmiPort");
        rmiPort = Utils.getRmiport(rmiPort);

        try {
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);

            tm = (TransactionManager) registry.lookup(rmiPort + TransactionManager.RMIName);
            System.out.println(myRMIName + "'s xids is Empty ? " + xids.isEmpty());
            for (Object xid1 : xids) {
                int xid = (Integer) xid1;
                System.out.println(myRMIName + " Re-enlist to TM with xid" + xid);
                System.out.println("here1");
                System.out.println(String.join("   ", registry.list()));
                tm.ping();
                System.out.println("here1-1");
                tm.enlist(xid, this);
                System.out.println("here2");
                if (dieTime.equals("AfterEnlist"))
                    dieNow();
                //                iter.remove();
            }
            System.out.println(myRMIName + " bound to TM");
        } catch (Exception e) {
            System.out.println("here3");
            System.err.println(myRMIName + " enlist error:" + e);
            return false;
        }

        return true;
    }

    public boolean dieNow() throws RemoteException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }

    protected RMTable loadTable(File file) {
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(new FileInputStream(file));
            return (RMTable) oin.readObject();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (oin != null)
                    oin.close();
            } catch (IOException e1) {
            }
        }
    }

    protected boolean storeTable(RMTable table, File file) {
        file.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(new FileOutputStream(file));
            oout.writeObject(table);
            oout.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (oout != null)
                    oout.close();
            } catch (IOException e1) {
            }
        }
    }

    protected RMTable getTable(int xid, String tablename) {
        Hashtable xidtables;
        synchronized (tables) {
            xidtables = (Hashtable) tables.get(xid);
            if (xidtables == null) {
                xidtables = new Hashtable();
                tables.put(xid, xidtables);
            }
        }
        synchronized (xidtables) {
            RMTable table = (RMTable) xidtables.get(tablename);
            if (table != null)
                return table;
            table = loadTable(new File("data/" + (xid == -1 ? "" : "" + xid + "/") + tablename));
            if (table == null) {
                if (xid == -1)
                    table = new RMTable(tablename, null, -1, lm);
                else {
                    table = new RMTable(tablename, getTable(tablename), xid, lm);
                }
            } else {
                if (xid != -1) {
                    table.setLockManager(lm);
                    table.setParent(getTable(tablename));
                }
            }
            xidtables.put(tablename, table);
            return table;
        }
    }

    protected RMTable getTable(String tablename) {
        return getTable(-1, tablename);
    }

    protected HashSet loadTransactionLogs() {
        File xidLog = new File("data/transactions.log");
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(new FileInputStream(xidLog));
            return (HashSet) oin.readObject();
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

    protected boolean storeTransactionLogs(HashSet xids) {
        File xidLog = new File("data/transactions.log");
//        xidLog.getParentFile().mkdirs();
        xidLog.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(new FileOutputStream(xidLog));
            oout.writeObject(xids);
            oout.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (oout != null)
                    oout.close();
            } catch (IOException e1) {
            }
        }
    }

    public Collection query(int xid, String tablename) throws DeadlockException, InvalidTransactionException,
            RemoteException {
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try {
            synchronized (xids) {
                xids.add(xid);
                storeTransactionLogs(xids);
            }

            getTransactionManager().enlist(xid, this);
        } catch (TransactionManagerUnaccessibleException e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        Collection result = new ArrayList();
        RMTable table = getTable(xid, tablename);
        synchronized (table) {
            for (Iterator iter = table.keySet().iterator(); iter.hasNext(); ) {
                Object key = iter.next();
                ResourceItem item = table.get(key);
                if (item != null && !item.isDeleted()) {
                    table.lock(key, LockManager.READ);
                    result.add(item);
                }
            }
            if (!result.isEmpty()) {
                if (!storeTable(table, new File("data/" + xid + "/" + tablename))) {
                    throw new RemoteException("System Error: Can't write table to disk!");
                }
            }
        }
        return result;
    }

    public ResourceItem query(int xid, String tablename, Object key) throws DeadlockException,
            InvalidTransactionException, RemoteException {
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try {
            synchronized (xids) {
                xids.add(xid);
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        } catch (TransactionManagerUnaccessibleException e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);
        ResourceItem item = table.get(key);
        if (item != null && !item.isDeleted()) {
            table.lock(key, LockManager.READ);
            if (!storeTable(table, new File("data/" + xid + "/" + tablename))) {
                throw new RemoteException("System Error: Can't write table to disk!");
            }
            return item;
        }
        return null;
    }

    public Collection query(int xid, String tablename, String indexName, Object indexVal) throws DeadlockException,
            InvalidTransactionException, InvalidIndexException, RemoteException {
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try {
            synchronized (xids) {
                xids.add(xid);
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        } catch (TransactionManagerUnaccessibleException e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        Collection result = new ArrayList();
        RMTable table = getTable(xid, tablename);
        synchronized (table) {
            for (Iterator iter = table.keySet().iterator(); iter.hasNext(); ) {
                Object key = iter.next();
                ResourceItem item = table.get(key);
                if (item != null && !item.isDeleted() && item.getIndex(indexName).equals(indexVal)) {
                    table.lock(key, LockManager.READ);
                    result.add(item);
                }
            }
            if (!result.isEmpty()) {
                if (!storeTable(table, new File("data/" + xid + "/" + tablename))) {
                    throw new RemoteException("System Error: Can't write table to disk!");
                }
            }
        }
        return result;
    }

    public boolean update(int xid, String tablename, Object key, ResourceItem newItem) throws DeadlockException,
            InvalidTransactionException, RemoteException {
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        if (!key.equals(newItem.getKey()))
            throw new IllegalArgumentException();

        try {
            synchronized (xids) {
                xids.add(xid);
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        } catch (TransactionManagerUnaccessibleException e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);
        ResourceItem item = table.get(key);
        if (item != null && !item.isDeleted()) {
            table.lock(key, LockManager.WRITE);
            table.put(newItem);
            if (!storeTable(table, new File("data/" + xid + "/" + tablename))) {
                throw new RemoteException("System Error: Can't write table to disk!");
            }
            return true;
        }
        return false;
    }

    public boolean insert(int xid, String tablename, ResourceItem newItem) throws DeadlockException,
            InvalidTransactionException, RemoteException {
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }

        try {
            synchronized (xids) {
                xids.add(xid);
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        } catch (TransactionManagerUnaccessibleException e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);
        ResourceItem item = table.get(newItem.getKey());
        if (item != null && !item.isDeleted()) {
            return false;
        }
        table.lock(newItem.getKey(), LockManager.WRITE);
        table.put(newItem);
        if (!storeTable(table, new File("data/" + xid + "/" + tablename))) {
            throw new RemoteException("System Error: Can't write table to disk!");
        }
        return true;
    }

    public boolean delete(int xid, String tablename, Object key) throws DeadlockException, InvalidTransactionException,
            RemoteException {
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }

        try {
            synchronized (xids) {
                xids.add(xid);
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        } catch (TransactionManagerUnaccessibleException e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);
        ResourceItem item = table.get(key);
        if (item != null && !item.isDeleted()) {
            table.lock(key, LockManager.WRITE);
            item = (ResourceItem) item.clone();
            item.delete();
            table.put(item);
            if (!storeTable(table, new File("data/" + xid + "/" + tablename))) {
                throw new RemoteException("System Error: Can't write table to disk!");
            }
            return true;
        }
        return false;
    }

    public int delete(int xid, String tablename, String indexName, Object indexVal) throws DeadlockException,
            InvalidTransactionException, InvalidIndexException, RemoteException {
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try {
            synchronized (xids) {
                xids.add(xid);
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        } catch (TransactionManagerUnaccessibleException e) {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        int n = 0;

        RMTable table = getTable(xid, tablename);
        synchronized (table) {
            for (Iterator iter = table.keySet().iterator(); iter.hasNext(); ) {
                Object key = iter.next();
                ResourceItem item = table.get(key);
                if (item != null && !item.isDeleted() && item.getIndex(indexName).equals(indexVal)) {
                    table.lock(item.getKey(), LockManager.WRITE);
                    item = (ResourceItem) item.clone();
                    item.delete();
                    table.put(item);
                    n++;
                }
            }
            if (n > 0) {
                if (!storeTable(table, new File("data/" + xid + "/" + tablename))) {
                    throw new RemoteException("System Error: Can't write table to disk!");
                }
            }
        }
        return n;
    }

    public boolean prepare(int xid) throws InvalidTransactionException, RemoteException {
        if (dieTime.equals("BeforePrepare"))
            dieNow();
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        if (dieTime.equals("AfterPrepare"))
            dieNow();
        return true;
    }

    public void commit(int xid) throws InvalidTransactionException, RemoteException {
        if (dieTime.equals("BeforeCommit"))
            dieNow();
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        Hashtable xidtables = (Hashtable) tables.get(xid);
        if (xidtables != null) {
            synchronized (xidtables) {
                for (Iterator iter = xidtables.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    RMTable xtable = (RMTable) entry.getValue();            // in memory
                    RMTable table = getTable(xtable.getTablename());        // load from file, flight, car...
                    for (Iterator iter2 = xtable.keySet().iterator(); iter2.hasNext(); ) {
                        Object key = iter2.next();
                        ResourceItem item = xtable.get(key);
                        if (item.isDeleted())
                            table.remove(item);
                        else
                            table.put(item);            // merge memory data into file
                    }
                    if (!storeTable(table, new File("data/" + entry.getKey())))
                        throw new RemoteException("Can't write table to disk");
                    new File("data/" + xid + "/" + entry.getKey()).delete();
                }
                new File("data/" + xid).delete();
                tables.remove(xid);
            }
        }

        if (!lm.unlockAll(xid))
            throw new RuntimeException();

        synchronized (xids) {
            xids.remove(xid);
        }
    }

    public void abort(int xid) throws InvalidTransactionException, RemoteException {
        if (dieTime.equals("BeforeAbort"))
            dieNow();
        if (xid < 0) {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        Hashtable xidtables = (Hashtable) tables.get(xid);
        if (xidtables != null) {
            synchronized (xidtables) {
                for (Iterator iter = xidtables.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    new File("data/" + xid + "/" + entry.getKey()).delete();
                }
                new File("data/" + xid).delete();
                tables.remove(xid);
            }
        }

        if (!lm.unlockAll(xid))
            throw new RuntimeException();

        synchronized (xids) {
            xids.remove(xid);
        }
    }

    public static void main(String[] args) {
        System.setSecurityManager(new RMISecurityManager());

        String rmiName = System.getProperty("rmiName");
        if (rmiName == null || rmiName.equals("")) {
            System.err.println("No RMI name given");
            System.exit(1);
        }

        String rmiPort = System.getProperty("rmiPort");

        try {
            RMIServerSocketFactory ssf = port -> new ServerSocket(port, 0, java.net.InetAddress.getLocalHost());
            RMIClientSocketFactory csf = Socket::new;
            LocateRegistry.createRegistry(Integer.parseInt(rmiPort), csf, ssf);

//            LocateRegistry.createRegistry(Integer.parseInt(rmiPort));
            System.out.println("registered");
        } catch (Exception e) {
            System.out.println("Port has registered.");
        }

        rmiPort = Utils.getRmiport(rmiPort);

        try {
            ResourceManagerImpl obj = new ResourceManagerImpl(rmiName);
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
            registry.rebind(rmiPort + rmiName, obj);
//            Naming.rebind(rmiPort + rmiName, obj);
            System.out.println(rmiName + " bound");
        } catch (Exception e) {
            System.err.println(rmiName + " not bound:" + e);
            System.exit(1);
        }
    }


}