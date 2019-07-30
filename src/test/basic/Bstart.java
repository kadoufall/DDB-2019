package test.basic;

import test.ConnectWC;
import transaction.WorkflowController;

public class Bstart {

    public static void main(String[] a) {

        WorkflowController wc = ConnectWC.connect();
        try {
            int xid = wc.start();
            System.out.println("Start");
        } catch (Exception e) {
            System.out.println("Bstart exception " + e.getMessage());
        }
    }
}
