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
			/*wc.addFlight(xid, "347", 100, 310);
			wc.addRooms(xid, "Stanford", 200, 150);	
			wc.addCars(xid, "SFO", 300, 30);
			wc.newCustomer(xid, "John");		
			wc.commit(xid);			
			
			xid = wc.start();
			wc.addFlight(xid, "347", 100, 620);
			wc.addRooms(xid, "Stanford", 200, 300);	
			wc.addCars(xid, "SFO", 300, 60);	
			wc.dieRMBeforeCommit("RMRooms");
			wc.dieRMBeforeCommit("RMCars");
			wc.commit(xid);
			////////////////launch RMRooms
			Connector.launch("RMRooms");
			////////////////launch RMCars
			Connector.launch("RMCars");
			wc.reconnect();

			xid = wc.start();
			int r1 = wc.queryFlight(xid, "347");
			check(100, r1);
			int r2 = wc.queryFlightPrice(xid, "347");
			check(310, r2);
			int r3 = wc.queryRooms(xid, "Stanford");
			check(200, r3);
			int r4 = wc.queryRoomsPrice(xid, "Stanford");
			check(150, r4);
			int r5 = wc.queryCars(xid, "SFO");
			check(300, r5);
			int r6 = wc.queryCarsPrice(xid, "SFO");
			check(30, r6);
			int r7 = wc.queryCustomerBill(xid, "John");
			check(0, r7);
			*/
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
