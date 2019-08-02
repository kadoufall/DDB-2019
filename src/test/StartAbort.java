package test;

import transaction.WorkflowController;

public class StartAbort{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try{
            int xid = wc.start();
            wc.abort(xid);
            System.out.println("Test pass.");
            Connector.cleanUpExit(0);
        }catch (Exception e) {
            System.out.println("Test fail:" + e.getMessage());
            Connector.cleanUpExit(0);
        }
    }
}
