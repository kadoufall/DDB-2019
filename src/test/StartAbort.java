package test;

import test.Connector;
import transaction.WorkflowController;

public class StartAbort{

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try{
            int xid;
            xid = wc.start();
            wc.abort(xid);
            System.out.println("Test pass.");
        }catch (Exception e) {
            System.out.println("Test fail:" + e.getMessage());
        }finally {
            Connector.cleanUpExit();
        }
    }
}
