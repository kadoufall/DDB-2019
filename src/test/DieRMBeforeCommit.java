package test;

import test.Connector;
import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;


public class DieRMBeforeCommit {
	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
		 try{
			int xid;

			xid = wc.start();
			wc.dieRMBeforeCommit("RMRooms");
			wc.dieRMBeforeCommit("RMCars");
			wc.addRooms(xid, "Shanghai", 1000, 999);
			wc.addCars(xid, "BYD", 1000, 879);			
			wc.commit(xid);				
			
			System.out.println("Test pass.");
			Connector.cleanUpExit(0);
		 }catch (Exception e) {
            System.out.println("Test fail:" + e);
            Connector.cleanUpExit(1);
        }
	}
	/**private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail");
        }
    }*/
}
