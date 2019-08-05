package test;

import transaction.WorkflowController;
import test.Connector;

public class DieRMBeforeAbort{
	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
		 try{
			int xid;

			xid = wc.start();	
			wc.dieRMBeforeAbort("RMRooms");	
			wc.dieRMBeforeAbort("RMCars");
			wc.addRooms(xid, "Stanford", 200, 150);	
			wc.addCars(xid, "SFO", 300, 30);	
			wc.abort(xid);	
			
			System.out.println("Test pass.");	
			Connector.cleanUpExit(0);
		 }catch(Exception e){
			 System.out.println("DieRMBeforeAbort Exception:" + e.getMessage());
			 //The transaction 1 aborted:commit prepare
			 Connector.cleanUpExit(1);
		 }
	}
	private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail.");
        }
    }
}
