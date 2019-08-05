package test;

import test.Connector;
import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


public class DieWC {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid;
            xid = wc.start();
            wc.addFlight(xid, "MU5377", 100, 500);
            wc.addRooms(xid, "BYD", 300, 350);
            wc.addCars(xid, "BYD", 100, 30);
            wc.newCustomer(xid, "CYLV");
            wc.commit(xid);

            xid = wc.start();
            //////////wc.reserveItinerary xid "John" (347) "SFO" true true
            List<String> flights = new ArrayList<>();
            flights.add("MU5377");
            wc.reserveItinerary(xid, "CYLV", flights, "BYD", true, true);
            try {
                wc.dieNow("WC");
            }catch (RemoteException e) {
                // e.printStackTrace();
            }
            /////////except java.rmi.RemoteException
            /////////launch WC
            Connector.launch("WC");
            wc = Connector.connectWC();
            wc.commit(xid);


            xid = wc.start();
            int r1 = wc.queryFlight(xid, "MU5377");
            check(99, r1);
            int r2 = wc.queryFlightPrice(xid, "MU5377");
            check(500, r2);
            int r3 = wc.queryRooms(xid, "BYD");
            check(299, r3);
            int r4 = wc.queryRoomsPrice(xid, "BYD");
            check(350, r4);
            int r5 = wc.queryCars(xid, "BYD");
            check(99, r5);
            int r6 = wc.queryCarsPrice(xid, "BYD");
            check(30, r6);
            int r7 = wc.queryCustomerBill(xid, "CYLV");
            check(880, r7);
            wc.commit(xid);

            System.out.println("Test pass."); 
            Connector.cleanUpExit(0);

        } catch (Exception e) {
            System.out.println("DieWC exception " + e.getMessage());
            Connector.cleanUpExit(1);
            /*if (e.getClass().getName().equals(TransactionAbortedException.class.getName())) {
                System.out.println("Test pass!");
            } else {
                System.out.println("Test fail:" + e);
            }*/
            //System.out.println("Test fail:" + e.getMessage());
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
