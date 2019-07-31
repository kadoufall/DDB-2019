package test;

import test.Connector;
import transaction.WorkflowController;

public class Bstart {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            System.out.println("Start");
        } catch (Exception e) {
            System.out.println("Bstart exception " + e.getMessage());
        } finally {
            Connector.cleanUpExit();
        }
    }
}
