package test;

import transaction.Utils;
import transaction.WorkflowController;

import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ConnectWC {
    public static WorkflowController connect() {
        String rmiPort = "3345";

        WorkflowController wc = null;

        try {
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
            rmiPort = Utils.getOriginRmiport(rmiPort);
            wc = (WorkflowController) registry.lookup(rmiPort + WorkflowController.RMIName);

        } catch (Exception e) {
            System.err.println("Cannot bind to WC: " + e.getMessage());
            System.exit(1);
        }
        return wc;
    }
}
