package test.basic;

import test.ConnectWC;
import transaction.WorkflowController;

public class Baddcmt {

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
        } catch (Exception e) {
            System.out.println("Baddcmt " + e.getMessage());
        }
    }
}
