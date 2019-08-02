package test;

import test.Connector;
import transaction.WorkflowController;
import transaction.exceptions.TransactionAbortedException;


public class DieTMAfterCommit {
	public static void main(String[] a){

		Connector.cleanData();
		Connector.launch("ALL");
		
		 WorkflowController wc = Connector.connectWC();
		 try{
			int xid;
			xid = wc.start();	 
			xid = wc.start();			
			wc.deleteRooms(xid, "handan", 100);
			wc.deleteCars(xid, "handan", 300);
			wc.dieTMAfterCommit();
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
			wc.dieTMAfterCommit();
			wc.commit(xid);
			////////// except java.rmi.RemoteException
			////////// launch TM
			Connector.launch("TM");
			wc.dieNow("RMFlights");
			/////////  launch RMFlights
			Connector.launch("RMFlights");
			wc.dieNow("RMRooms");
			/////////  launch RMRooms
			Connector.launch("RMRooms");
			wc.dieNow("RMCars");
			/////////  launch RMCars
			Connector.launch("RMCars");
			wc.reconnect();

			xid = wc.start();
			int r1 = wc.queryFlight(xid, "347");
			check(200, r1);
			int r2 = wc.queryFlightPrice(xid, "347");
			check(620, r2);
			int r3 = wc.queryRooms(xid, "Stanford");
			check(400, r3);
			int r4 = wc.queryRoomsPrice(xid, "Stanford");
			check(300, r4);
			int r5 = wc.queryCars(xid, "SFO");
			check(600, r5);
			int r6 = wc.queryCarsPrice(xid, "SFO");
			check(60, r6);
			int r7 = wc.queryCustomerBill(xid, "John");
			check(0, r7);*/
			System.out.println("Test pass.");		
		 }catch(Exception e){
			//System.out.println("Test fail:"+e.getMessage());
			if (e.getClass().getName().equals(TransactionAbortedException.class.getName())) {
                System.out.println("Test pass!");
            } else {
                System.out.println("Test fail:" + e);
            }
		 }finally {
            Connector.cleanUpExit();
        }
	}
	private static void check(int expect, int real) {
        if (expect != real) {
            System.out.println(expect + " " + real);
            System.err.println("Test fail");
        }
    }
}
