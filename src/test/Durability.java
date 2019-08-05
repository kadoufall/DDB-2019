package test;

import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;

import java.rmi.RemoteException;

public class Durability{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "MU5377", 100, 500);
            wc.addRooms(xid, "HANDAN", 300, 350);
            wc.dieTMAfterCommit();
            //wc.commit(xid);
            try {
                wc.commit(xid);
            } catch (RemoteException e) {
                // e.printStackTrace();
            }
            Connector.launch("TM");
            wc.reconnect();
            int r1 = wc.queryFlight(xid, "MU5377");
            check(100, r1);
            int r2 = wc.queryRooms(xid, "HANDAN");
            check(300, r2);
            try {
                wc.commit(xid);
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }
            System.out.println("Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.err.println("Test fail:" + e.getMessage());
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
