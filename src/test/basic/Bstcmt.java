package test.basic;

import test.ConnectWC;
import transaction.WorkflowController;

public class Bstcmt {

    public static void main(String[] a) {

        WorkflowController wc = ConnectWC.connect();
        try {
            int xid;
            xid = wc.start();
            wc.commit(xid);
        } catch (Exception e) {
            System.out.println("insert data exception " + e.getMessage());
        }
    }
}