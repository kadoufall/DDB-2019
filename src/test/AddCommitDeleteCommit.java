package test;

import transaction.WorkflowController;

public class AddCommitDeleteCommit{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            //wc.addFlight(xid, "MU5377", 100, 500);
            wc.addRooms(xid, "shanghai", 300, 350);
            wc.addCars(xid, "BYD", 100, 30);
            wc.newCustomer(xid, "CYLV");
            wc.commit(xid);

            xid = wc.start();
            //wc.addFlight(xid, "MU5377", 100, 420);
            wc.deleteRooms(xid, "shanghai", 10);
            wc.deleteCars(xid, "BYD", 10);
            wc.commit(xid);

            xid = wc.start();
            int r1 = wc.queryRooms(xid, "shanghai");
            check(290, r1);
            int r2 = wc.queryCars(xid, "BYD");
            check(90, r2);
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
