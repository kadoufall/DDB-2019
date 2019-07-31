
import test.Connector;

import transaction.WorkflowController;

public class Baddabtrd {
    public static void main(String[] args) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
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
            wc.abort(xid);

            xid = wc.start();
            wc.queryFlight(xid, "347");
            wc.queryFlightPrice(xid, "347");
            wc.queryRooms(xid, "Stanford");
            wc.queryRoomsPrice(xid, "Stanford");
            wc.queryCars(xid, "SFO");
            wc.queryCarsPrice(xid, "SFO");
            wc.queryCustomerBill(xid, "John");

		/*
	    	int xid1 = wc.start();
	    	if (!wc.addFlight(xid1, "347", 230, 999)) {
	    	    System.err.println("Add flight failed");
	    	}else{
	    		System.err.println("Add flight successful");
	    	}
	    	  
	    	 new Thread(){// for query
	    	    public void run(){
	    	    	WorkflowController wc = Connector.connect();
	    			try {
	    			   int xid2 = wc.start();
	    			   System.out.println("Flight 347 has " +
	    			     			wc.queryFlight(xid2, "347") +  " seats.");
	    			   if (!wc.commit(xid2)) {
	    			     	System.err.println(xid2+"\tCommit failed");
	    			   } 
	    			}catch (Exception e) {
	    			   System.err.println("query thread exception: " + e);
	    			}}//run 
	    	    }.start();
	    	    
	    	Thread.sleep(1000);
	    	if (!wc.commit(xid1)) {
	    	    System.err.println(xid1+"\tCommit failed");
	    	}
		*/

        } catch (Exception e) {
            System.err.println("client Received exception:" + e);
            //System.exit(1);
        }

    }
}
