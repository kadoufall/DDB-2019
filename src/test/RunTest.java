package test;

import java.io.*;

public class RunTest {

    public static void main(String[] args) throws IOException {
        String testName = System.getProperty("testName");

        File dataDir = new File("results/"+testName);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        System.out.println("Launching test " + testName);
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(new String[]{
                    "sh",
                    "-c",
                    "java -classpath .. -DrmiPort=" + System.getProperty("rmiPort") +
                            " -DtestName=" + testName +
                            " -Djava.security.policy=./security-policy test." + testName +
                            " >results/" + testName + "/" + "out.log" +
                            " 2>results/" + testName + "/" + "err.log"});
        } catch (IOException e) {
            System.err.println("Cannot launch Test: " + e);
            System.exit(1);
        }

        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            System.err.println("WaitFor interrupted.");
            System.exit(1);
        }

        int exitVal = proc.exitValue();
        if (exitVal == 0) {
            System.out.println("Test " + testName + " passed.");
        } else if (exitVal == 2) {
            System.out.println("Test " + testName + " failed.");
        } else {
            SequenceInputStream sis = new SequenceInputStream(proc.getInputStream(), proc.getErrorStream());
            InputStreamReader inst = new InputStreamReader(sis);
            BufferedReader br = new BufferedReader(inst);

            String res = null;
            StringBuilder sb = new StringBuilder();
            while ((res = br.readLine()) != null) {
                System.out.println(res);
                sb.append(res).append("\n");
            }
            br.close();
            System.out.println(sb);

            System.err.println("Test " + testName + " errored (" + exitVal + ")");
            System.exit(1);
        }
    }
}
