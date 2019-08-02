package test;

import test.Connector;
import transaction.WorkflowController;

public class StartCommit {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid;
            xid = wc.start();
            wc.commit(xid);
            System.out.println("Test pass.");
        } catch (Exception e) {
            System.out.println("Test fail:" + e.getMessage());
        } finally {
            Connector.cleanUpExit();
        }
    }
}
