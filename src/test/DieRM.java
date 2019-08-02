package test;

import transaction.WorkflowController;

public class DieRM {
	
	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
		 try{
			int xid =  wc.start();
			wc.addFlight(xid, "347", 100, 310);
			wc.addRooms(xid, "Stanford", 200, 150);	
			wc.addCars(xid, "SFO", 300, 30);
			wc.newCustomer(xid, "John");		
			wc.commit(xid);			
			
			xid = wc.start();	
			//////////wc.reserveItinerary xid "John" (347) "SFO" true false
			wc.addRooms(xid, "Stanford", 200, 300);
			wc.dieNow("RMCars");
			/////////launch RMCars
			wc.reconnect();
			wc.commit(xid);
			/////////except transaction.exceptions.TransactionAbortedException

			xid = wc.start();
			wc.queryFlight(xid, "347");
			wc.queryFlightPrice(xid, "347");
			wc.queryRooms(xid, "Stanford");
			wc.queryRoomsPrice(xid, "Stanford");
			wc.queryCars(xid, "SFO");
			wc.queryCarsPrice(xid, "SFO");
			wc.queryCustomerBill(xid, "John");
		 }catch(Exception e){
			 System.out.println("dieRM exception "+e.getMessage());
		 }
	}
}
