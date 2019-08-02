package test;

import transaction.WorkflowController;

import java.rmi.RemoteException;

public class DieAll {

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

            try {
                wc.dieNow("ALL");
            } catch (RemoteException e) {
                // e.printStackTrace();
            }

            Connector.launch("ALL");
            wc = Connector.connectWC();

            xid = wc.start();
            int ret1 = wc.queryFlight(xid, "347");
            check(100, ret1, "queryFlight");

            int ret2 = wc.queryFlightPrice(xid, "347");
            check(310, ret2, "queryFlightPrice");

            int ret3 = wc.queryRooms(xid, "Stanford");
            check(200, ret3, "queryRooms");

            int ret4 = wc.queryRoomsPrice(xid, "Stanford");
            check(150, ret4, "queryRoomsPrice");

            int ret5 = wc.queryCars(xid, "SFO");
            check(300, ret5, "queryCars");

            int ret6 = wc.queryCarsPrice(xid, "SFO");
            check(30, ret6, "queryCarsPrice");

            int ret7 = wc.queryCustomerBill(xid, "John");
            check(0, ret7, "queryCustomerBill");

            wc.commit(xid);
            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("DieAll exception " + e.getMessage());
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
