package test;

import transaction.WorkflowController;

public class DieRMAfterEnlist {
    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "347", 100, 310);
            wc.addRooms(xid, "Stanford", 200, 150);
            wc.addCars(xid, "SFO", 300, 30);
            wc.newCustomer(xid, "John");
            wc.commit(xid);

            xid = wc.start();
            wc.dieRMAfterEnlist("RMRooms");
            wc.addFlight(xid, "347", 100, 620);

            try {
                wc.reserveRoom(xid, "John", "Stanford");
            } catch (Exception e) {
                // e.printStackTrace();
            }

            ///////////////launch RMRooms
            Connector.launch("RMRooms");
            wc.reconnect();

            try {
                wc.commit(xid);
            } catch (Exception e) {
                // e.printStackTrace();
            }

            xid = wc.start();
            int r1 = wc.queryFlight(xid, "347");
            check(100, r1, "queryFlight");

            int r2 = wc.queryFlightPrice(xid, "347");
            check(310, r2, "queryFlightPrice");

            int r3 = wc.queryRooms(xid, "Stanford");
            check(200, r3, "queryRooms");

            int r4 = wc.queryRoomsPrice(xid, "Stanford");
            check(150, r4, "queryRoomsPrice");

            int r5 = wc.queryCars(xid, "SFO");
            check(300, r5, "queryCars");

            int r6 = wc.queryCarsPrice(xid, "SFO");
            check(30, r6, "queryCarsPrice");

            int r7 = wc.queryCustomerBill(xid, "John");
            check(0, r7, "queryCustomerBill");

            wc.commit(xid);

            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DieRMAfterEnlist exception " + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
