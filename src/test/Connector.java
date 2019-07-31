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
    private static String rmiPort = "3345";

    public static void cleanData() {
        try {
            Runtime.getRuntime().exec("rm -rf ./data/*");
        } catch (IOException e) {
            e.printStackTrace();
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
                    String execStr = "java -classpath .. -DrmiPort=" + rmiPort +
                            " -DrmiName=" + rmiNames[i] +
                            " -Djava.security.policy=./security-policy transaction." + classNames[i];
                    Process process = Runtime.getRuntime().exec(execStr);

                    printMessage(process.getInputStream());
                    printMessage(process.getErrorStream());
//                    int value = process.waitFor();
//                    System.out.println(value);
                } catch (Exception e) {
                    System.err.println("Cannot launch " + rmiNames[i] + ": " + e);
                    cleanUpExit();
                }

                System.out.println(rmiNames[i] + " launched");

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted.");
                    System.exit(1);
                }
            }
        }
    }

    private static void printMessage(final InputStream input) {
        new Thread(() -> {
            Reader reader = new InputStreamReader(input);
            BufferedReader bf = new BufferedReader(reader);
            String line;
            try {

                while ((line = bf.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static WorkflowController connectWC() {

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

    public static void cleanUpExit() {
        try {
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
            rmiPort = Utils.getOriginRmiport(rmiPort);
            WorkflowController wc = (WorkflowController) registry.lookup(rmiPort + WorkflowController.RMIName);
            wc.dieNow("ALL");
        } catch (Exception e) {
//            e.printStackTrace();
            System.exit(0);
        }
    }
}
