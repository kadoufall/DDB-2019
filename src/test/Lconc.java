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
            wc.addFlight(xid1, "MU5377", 100, 500);

            wc.addRooms(xid2, "SHANGHAI", 300, 350);

            wc.addCars(xid1, "BYD", 100, 30);

            wc.commit(xid2);

            wc.commit(xid1);

            int xid3 = wc.start();
            int ret1 = wc.queryFlight(xid3, "MU5377");
            check(100, ret1, "queryFlight");

            int ret2 = wc.queryFlightPrice(xid3, "MU5377");
            check(500, ret2, "queryFlightPrice");

            int ret3 = wc.queryRooms(xid3, "SHANGHAI");
            check(300, ret3, "queryRooms");

            int ret4 = wc.queryRoomsPrice(xid3, "SHANGHAI");
            check(350, ret4, "queryRoomsPrice");

            int ret5 = wc.queryCars(xid3, "BYD");
            check(100, ret5, "queryCars");

            int ret6 = wc.queryCarsPrice(xid3, "BYD");
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
