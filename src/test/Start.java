package test;

import transaction.WorkflowController;

public class Start {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            System.out.println("Test pass.");
        } catch (Exception e) {
            System.out.println("Test fail:" + e.getMessage());
        } finally {
            Connector.cleanUpExit(0);
        }
    }
}
