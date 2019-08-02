package test;

import transaction.WorkflowController;
import test.Connector;
import transaction.exceptions.TransactionAbortedException;
import java.util.ArrayList;
import java.util.List;

public class DienpRM {

    public static void main(String[] a) {
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
            /*wc.addFlight(xid, "347", 100, 310);
            wc.addRooms(xid, "Stanford", 200, 150);
            wc.addCars(xid, "SFO", 300, 30);
            wc.newCustomer(xid, "John");*/
            wc.commit(xid);

            xid = wc.start();
            //////////wc.reserveItinerary xid "John" (347) "SFO" false false
            List<String> flights = new ArrayList<>();
            flights.add("347");
            wc.reserveItinerary(xid, "CYLV", flights, "BYD", false, false);
            wc.addRooms(xid, "shanghai", 200, 300);
            wc.dieNow("RMCars");
            /////////launch RMCars
            Connector.launch("RMCars");
            wc.reconnect();
            wc.commit(xid);


            xid = wc.start();
            int r1 = wc.queryFlight(xid, "MU5377");
            check(100, r1);
            int r2 = wc.queryFlightPrice(xid, "MU5377");
            check(310, r2);
            int r3 = wc.queryRooms(xid, "shanghai");
            check(500, r3);
            int r4 = wc.queryRoomsPrice(xid, "shanghai");
            check(300, r4);
            int r5 = wc.queryCars(xid, "BYD");
            check(100, r5);
            int r6 = wc.queryCarsPrice(xid, "BYD");
            check(30, r6);
            int r7 = wc.queryCustomerBill(xid, "CYLV");
            check(0, r7);
            System.out.println("Test pass."); 
        } catch (Exception e) {
            //System.out.println("Test fail:" + e.getMessage());
            if (e.getClass().getName().equals(TransactionAbortedException.class.getName())) {
                System.out.println("Test pass!");
            } else {
                System.out.println("Test fail:" + e);
            }
            //System.out.println("Test fail:"+e.getMessage());
            //The transaction 1 aborted:commit prepare
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
