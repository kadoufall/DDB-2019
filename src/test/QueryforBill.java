package test;

import transaction.WorkflowController;

public class QueryforBill{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "MU5377", 100, 500);
            wc.addRooms(xid, "shanghai", 300, 350);
            wc.addCars(xid, "BYD", 100, 30);
            wc.newCustomer(xid, "CYLV");
            wc.commit(xid);

            xid = wc.start();
            wc.reserveFlight(xid, "CYLV", "MU5377");
            wc.reserveRoom(xid, "CYLV", "shanghai");
            wc.reserveCar(xid, "CYLV", "BYD");
            wc.commit(xid);

            xid = wc.start();

            int r1 = wc.queryCustomerBill(xid, "CYLV");
            check(880, r1);
            
            wc.commit(xid);

            System.out.println("Test pass");
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
