package test;

import transaction.ResourceManager;
import transaction.TransactionManager;
import transaction.Utils;
import transaction.WorkflowController;

import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Connector {

    public static void cleanData() {
        try {
            if (Runtime.getRuntime().exec("rm -rf data").waitFor() != 0) {
                System.err.println("Clean data not successful");
            }
        } catch (IOException e) {
            System.err.println("Cannot clean data: " + e);
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("WaitFor interrupted.");
            System.exit(1);
        }
    }

    public static void launch(String who) {
        String[] rmiNames = new String[]{TransactionManager.RMIName,
                ResourceManager.RMINameFlights,
                ResourceManager.RMINameRooms,
                ResourceManager.RMINameCars,
                ResourceManager.RMINameCustomers,
                WorkflowController.RMIName};
        String[] classNames = new String[]{"TransactionManagerImpl",
                "ResourceManagerImpl",
                "ResourceManagerImpl",
                "ResourceManagerImpl",
                "ResourceManagerImpl",
                "WorkflowControllerImpl"};

        for (int i = 0; i < rmiNames.length; i++) {
            if (who.equals(rmiNames[i]) || who.equals("ALL")) {
                try {
                    Runtime.getRuntime().exec(new String[]{
                            "sh",
                            "-c",
                            "java -classpath .. -DrmiPort=" + System.getProperty("rmiPort") +
                                    " -DrmiName=" + rmiNames[i] +
                                    " -Djava.security.policy=./security-policy transaction." + classNames[i] +
                                    " >>" + "results/" + System.getProperty("testName") + "/" + rmiNames[i] + ".log" + " 2>&1"});
                } catch (Exception e) {
                    System.err.println("Cannot launch " + rmiNames[i] + ": " + e);
                    cleanUpExit(1);
                }

                System.out.println(rmiNames[i] + " launched");

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted.");
                    System.exit(1);
                }
            }
        }
    }

    public static WorkflowController connectWC() {
        String rmiPort = System.getProperty("rmiPort");

        WorkflowController wc = null;

        try {
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
            rmiPort = Utils.getOriginRmiport(rmiPort);

            wc = (WorkflowController) registry.lookup(rmiPort + WorkflowController.RMIName);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Cannot bind to WC: " + e.getMessage());
            System.exit(1);
        }
        return wc;
    }

    public static void cleanUpExit(int status) {
        String rmiPort = System.getProperty("rmiPort");
        try {
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
            rmiPort = Utils.getOriginRmiport(rmiPort);
            WorkflowController wc = (WorkflowController) registry.lookup(rmiPort + WorkflowController.RMIName);
            wc.dieNow("ALL");
        } catch (Exception e) {
//            e.printStackTrace();
            System.exit(status);
        }
    }
}
