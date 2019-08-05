package test;

import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;

public class DieRMAfterPrepare {
	public static void main(String[] a){
		Connector.cleanData();
		Connector.launch("ALL");

		WorkflowController wc = Connector.connectWC();
		 try{
			int xid;

			xid = wc.start();			
			wc.addFlight(xid, "MU5377", 100, 500);
            wc.addRooms(xid, "HANDAN", 300, 350);
            wc.addCars(xid, "BYD", 100, 30);
            wc.newCustomer(xid, "CYLV");		
			wc.commit(xid);			
			
			xid = wc.start();
			wc.addFlight(xid, "MU5377", 100, 520);
			wc.addRooms(xid, "HANDAN", 200, 300);	
			wc.addCars(xid, "BYD", 300, 60);	
			wc.dieRMAfterPrepare("RMFlights");
			//wc.commit(xid);
			try {
                wc.commit(xid);
            } catch (TransactionAbortedException e) {
                // e.printStackTrace();
            }
			////////////////except transaction.exceptions.TransactionAbortedException
			////////////////launch RMFlights
			Connector.launch("RMFlights");
			wc.reconnect();

			xid = wc.start();
			int ret1 = wc.queryFlight(xid, "MU5377");
            check(100, ret1, "queryFlight");

            int ret2 = wc.queryFlightPrice(xid, "MU5377");
            check(500, ret2, "queryFlightPrice");

            int ret3 = wc.queryRooms(xid, "HANDAN");
            check(300, ret3, "queryRooms");

            int ret4 = wc.queryRoomsPrice(xid, "HANDAN");
            check(350, ret4, "queryRoomsPrice");

            int ret5 = wc.queryCars(xid, "BYD");
            check(100, ret5, "queryCars");

            int ret6 = wc.queryCarsPrice(xid, "BYD");
            check(30, ret6, "queryCarsPrice");

            int ret7 = wc.queryCustomerBill(xid, "CYLV");
            check(0, ret7, "queryCustomerBill");
            wc.commit(xid);

			Connector.cleanUpExit(0);
		 }catch(Exception e){
			 System.out.println("DieRMAfterPrepare exception "+e.getMessage());
			 Connector.cleanUpExit(1);
		 }
	}
	private static void check(int expect, int real, String method) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail: " + method);
            Connector.cleanUpExit(1);
        }
    }
}
