package test;

import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;

public class Isolation{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid1 = wc.start();
            wc.addFlight(xid1, "MU5377", 100, 500);
            wc.commit(xid1);
            xid1 = wc.start();
            wc.addFlight(xid1, "MU5377", 100, 450);
            int xid2 = wc.start();
            try {
                wc.addFlight(xid2, "MU5377", 200, 300);
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }
            int r1 = wc.queryFlight(xid1, "MU5377");
            check(200, r1);
            wc.commit(xid1);
            xid2 = wc.start();
            wc.addFlight(xid2, "MU5377", 400, 3000);
            int r2 = wc.queryFlight(xid2, "MU5377");
            check(600, r2);
            wc.commit(xid2);
            System.out.println("Test pass.");
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.err.println("Test fail:" + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real) {
        if (expect != real) {
            System.err.println(expect + " " + real);
            System.err.println("Test fail");
            Connector.cleanUpExit(1);
        }
    }
}
