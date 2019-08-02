package test;

import test.Connector;
import transaction.WorkflowController;

public class QueryforDataAddAbort {
    public static void main(String[] args) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid;
            xid = wc.start();
            wc.addFlight(xid, "MU5377", 100, 500);
            wc.addRooms(xid, "shanghai", 300, 350);
            wc.addCars(xid, "BYD", 100, 30);
            wc.newCustomer(xid, "CYLV");
            wc.commit(xid);

            xid = wc.start();
            wc.addFlight(xid, "MU5377", 100, 620);
            wc.addRooms(xid, "shanghai", 200, 300);
            wc.addCars(xid, "BYD", 300, 60);
            wc.abort(xid);

            xid = wc.start();
            int r1 = wc.queryFlight(xid, "MU5377");
            check(100, r1);
            int r2 = wc.queryFlightPrice(xid, "MU5377");
            check(310, r2);
            int r3 = wc.queryRooms(xid, "shanghai");
            check(200, r3);
            int r4 = wc.queryRoomsPrice(xid, "shanghai");
            check(150, r4);
            int r5 = wc.queryCars(xid, "BYD");
            check(300, r5);
            int r6 = wc.queryCarsPrice(xid, "BYD");
            check(30, r6);
            int r7 = wc.queryCustomerBill(xid, "CYLV");
            check(0, r7);
            wc.commit(xid);
            System.out.println("Test pass:" + QueryforDataAddAbort.class.getName());

        } catch (Exception e) {
            System.err.println("Test fail:" + e);
            //System.exit(1);
        }finally {
            Connector.cleanUpExit();
        }
    }
    private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail");
        }
    }
}
