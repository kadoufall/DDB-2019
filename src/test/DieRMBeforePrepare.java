package test;

import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;

public class DieRMBeforePrepare {
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
            wc.addFlight(xid, "347", 100, 620);
            wc.addRooms(xid, "Stanford", 200, 300);
            wc.addCars(xid, "SFO", 300, 60);
            wc.dieRMBeforePrepare("RMFlights");

            try {
                wc.commit(xid);
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }

            ////////////////launch RMFlights
            Connector.launch("RMFlights");
            wc.reconnect();

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

            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DieRMBeforePrepare exception " + e.getMessage());
            Connector.cleanUpExit(1);
        }
    }

    private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.err.println("Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
