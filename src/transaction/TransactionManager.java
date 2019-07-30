package transaction;

import transaction.exceptions.InvalidTransactionException;
import transaction.exceptions.TransactionAbortedException;

import java.rmi.*;

/**
 * Interface for the Transaction Manager of the Distributed Travel
 * Reservation System.
 * <p>
 * Unlike WorkflowController.java, you are supposed to make changes
 * to this file.
 */

public interface TransactionManager extends Remote {
    /**
     * The RMI name a TransactionManager binds to.
     */
    public static final String RMIName = "TM";

    /**
     * The transaction status
     */
    public static final String NEW = "NEW";
    public static final String PREPARED = "PREPARED";
    public static final String COMMITTED = "COMMITTED";
    public static final String ABORTED = "ABORTED";

    public int start() throws RemoteException;

    public boolean commit(int xid)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    public void abort(int xid)
            throws RemoteException, InvalidTransactionException;

    public void setDieTime(String time) throws RemoteException;

    public boolean dieNow() throws RemoteException;

    public void ping() throws RemoteException;

    public void enlist(int xid, ResourceManager rm)
            throws RemoteException, InvalidTransactionException;


}
