package test.die;

import transaction.ResourceManager;
import transaction.WorkflowController;
import test.ConnectWC;

public class DieWC {

    public static void main(String[] a) {
        WorkflowController wc = ConnectWC.connect();
        try {
            int xid;

            xid = wc.start();
            wc.addFlight(xid, "347", 100, 310);
            wc.addRooms(xid, "Stanford", 200, 150);
            wc.addCars(xid, "SFO", 300, 30);
            wc.newCustomer(xid, "John");
            wc.commit(xid);

            xid = wc.start();
            //////////wc.reserveItinerary xid "John" (347) "SFO" true true
            wc.dieNow("WC");
            /////////except java.rmi.RemoteException
            /////////launch WC
            wc.commit(xid);


            xid = wc.start();
            wc.queryFlight(xid, "347");
            wc.queryFlightPrice(xid, "347");
            wc.queryRooms(xid, "Stanford");
            wc.queryRoomsPrice(xid, "Stanford");
            wc.queryCars(xid, "SFO");
            wc.queryCarsPrice(xid, "SFO");
            wc.queryCustomerBill(xid, "John");
        } catch (Exception e) {
            System.out.println("DieWC exception " + e.getMessage());
        }
    }
}
