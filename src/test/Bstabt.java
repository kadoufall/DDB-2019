package test;

import test.Connector;
import transaction.WorkflowController;

public class Bstabt {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid;
            xid = wc.start();
            wc.abort(xid);
        } catch (Exception e) {
            System.out.println("insert data exception " + e.getMessage());
        }
    }
}
