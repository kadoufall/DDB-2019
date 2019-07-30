package test.die.dieRM;

import transaction.WorkflowController;
import test.ConnectWC;

public class DieRMBeforeCommit {
	public static void main(String[] a){
		WorkflowController wc = ConnectWC.connect();
		 try{
			int xid;

			xid = wc.start();			
			wc.addFlight(xid, "347", 100, 310);
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
			////////////////launch RMCars
			wc.reconnect();

			xid = wc.start();
			wc.queryFlight(xid, "347");
			wc.queryFlightPrice(xid, "347");
			wc.queryRooms(xid, "Stanford");
			wc.queryRoomsPrice(xid, "Stanford");
			wc.queryCars(xid, "SFO");
			wc.queryCarsPrice(xid, "SFO");
			wc.queryCustomerBill(xid, "John");		
			
		 }catch(Exception e){
			 System.out.println("DieRMBeforeCommit exception "+e.getMessage());
		 }
	}
}
