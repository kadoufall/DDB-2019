package test;

import test.Connector;
import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;
import java.rmi.RemoteException;

public class Atomicity{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {

            int xid = wc.start();
            wc.addFlight(xid, "MU5377", 100, 500);
            wc.dieTMBeforeCommit();

            try {
                wc.commit(xid);
            } catch (RemoteException e) {
                // e.printStackTrace();
                /*if (e.getClass().getName().equals(RemoteException.class.getName())) {
                    System.out.println("Test pass!");
                } else {
                    System.out.println("Test fail:" + e);
                }*/
            }
            
            Connector.launch("TM");
            
            //xid = wc.start();
            int r1 = wc.queryFlight(xid, "MU5377");
            check(-1, r1);
            wc.commit(xid);
            
            System.out.println("Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
