package test;

import transaction.WorkflowController;

public class DieRMAfterPrepare {
	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
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
			wc.dieRMAfterPrepare("RMFlights");
			wc.commit(xid);
			////////////////except transaction.exceptions.TransactionAbortedException
			////////////////launch RMFlights
			wc.reconnect();

			xid = wc.start();
			wc.queryFlight(xid, "347");
			wc.queryFlightPrice(xid, "347");
			wc.queryRooms(xid, "Stanford");
			wc.queryRoomsPrice(xid, "Stanford");
			wc.queryCars(xid, "SFO");
			wc.queryCarsPrice(xid, "SFO");
			wc.queryCustomerBill(xid, "John");

			 Connector.cleanUpExit(0);
		 }catch(Exception e){
			 System.out.println("DieRMAfterPrepare exception "+e.getMessage());
			 Connector.cleanUpExit(1);
		 }
	}
}
