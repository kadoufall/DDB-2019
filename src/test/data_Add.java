package test;

import transaction.WorkflowController;

public class data_Add{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "MU5377", 100, 500);
            wc.addRooms(xid, "shanghai", 300, 350);
            wc.addCars(xid, "BYD", 100, 30);
            wc.newCustomer(xid, "CYLV");
            wc.commit(xid);

            System.out.println("Test pass.");
        } catch (Exception e) {
            System.out.println("Test fail:" +e.getMessage());
        } finally {
            Connector.cleanUpExit();
        }
    }
}
