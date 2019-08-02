package test;

import test.Connector;
import transaction.WorkflowController;
import java.util.ArrayList;
import java.util.List;
import transaction.exceptions.TransactionAbortedException;


public class DieWC {

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
            wc.commit(xid);

            xid = wc.start();
            //////////wc.reserveItinerary xid "John" (347) "SFO" true true
            List<String> flights = new ArrayList<>();
            flights.add("MU5377");
            wc.reserveItinerary(xid, "CYLV", flights, "BYD", true, true);
            wc.dieNow("WC");
            /////////except java.rmi.RemoteException
            /////////launch WC
            Connector.launch("WC");
            wc.commit(xid);


            xid = wc.start();
            int r1 = wc.queryFlight(xid, "347");
            check(99, r1);
            int r2 = wc.queryFlightPrice(xid, "347");
            check(310, r2);
            int r3 = wc.queryRooms(xid, "Stanford");
            check(299, r3);
            int r4 = wc.queryRoomsPrice(xid, "Stanford");
            check(350, r4);
            int r5 = wc.queryCars(xid, "SFO");
            check(99, r5);
            int r6 = wc.queryCarsPrice(xid, "SFO");
            check(30, r6);
            int r7 = wc.queryCustomerBill(xid, "John");
            check(880, r7);
            System.out.println("Test pass."); 
        } catch (Exception e) {
            if (e.getClass().getName().equals(TransactionAbortedException.class.getName())) {
                System.out.println("Test pass!");
            } else {
                System.out.println("Test fail:" + e);
            }
            //System.out.println("Test fail:" + e.getMessage());
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
