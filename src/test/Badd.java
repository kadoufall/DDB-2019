package test;

import transaction.WorkflowController;

public class Badd {

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

            System.out.println("Test pass");
        } catch (Exception e) {
            System.out.println("Test fail:" +e.getMessage());
        } finally {
            Connector.cleanUpExit();
        }
    }
}
