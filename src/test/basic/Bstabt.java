package test.basic;

import test.ConnectWC;
import transaction.WorkflowController;

public class Bstabt {

    public static void main(String[] a) {

        WorkflowController wc = ConnectWC.connect();
        try {
            int xid;
            xid = wc.start();
            wc.abort(xid);
        } catch (Exception e) {
            System.out.println("insert data exception " + e.getMessage());
        }
    }
}
