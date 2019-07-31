import test.Connector;
import transaction.WorkflowController;

public class Baddabt {

    public static void main(String[] a) {
        Connector.cleanData();
        Connector.launch("ALL");

        WorkflowController wc = Connector.connectWC();
        try {
            int xid = wc.start();
            wc.addFlight(xid, "347", 100, 310);
            wc.addRooms(xid, "Stanford", 200, 150);
            wc.addCars(xid, "SFO", 300, 30);
            wc.newCustomer(xid, "John");
            wc.abort(xid);
        } catch (Exception e) {
            System.out.println("insert data exception " + e.getMessage());
        } finally {
            Connector.cleanUpExit();
        }
    }
}
