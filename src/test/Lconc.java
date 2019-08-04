package test;

import transaction.WorkflowController;

public class Lconc {
    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid1 = wc.start();
            int xid2 = wc.start();
            wc.addFlight(xid1, "347", 100, 310);

            wc.addRooms(xid2, "Stanford", 200, 150);

            wc.addCars(xid1, "SFO", 300, 30);

            wc.commit(xid2);

            wc.commit(xid1);

            int xid3 = wc.start();
            int ret1 = wc.queryFlight(xid3, "347");
            check(100, ret1, "queryFlight");

            int ret2 = wc.queryFlightPrice(xid3, "347");
            check(310, ret2, "queryFlightPrice");

            int ret3 = wc.queryRooms(xid3, "Stanford");
            check(200, ret3, "queryRooms");

            int ret4 = wc.queryRoomsPrice(xid3, "Stanford");
            check(150, ret4, "queryRoomsPrice");

            int ret5 = wc.queryCars(xid3, "SFO");
            check(300, ret5, "queryCars");

            int ret6 = wc.queryCarsPrice(xid3, "SFO");
            check(30, ret6, "queryCarsPrice");

            Connector.cleanUpExit(0);
        } catch (Exception e) {
            System.out.println("Lconc exception " + e.getMessage());
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
