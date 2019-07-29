package transaction;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utils {

    public static String getHostname() {
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return hostname;
    }

    public static String getRmiport(String rmiPort) {
        String ret = "";
        if (rmiPort == null) {
            ret = "";
        } else if (!rmiPort.equals("")) {
            String hostname = "";
            try {
                hostname = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            ret = "rmi//" + hostname + ":" + rmiPort + "/";
        }
        return ret;
    }

    public static String getOriginRmiport(String rmiPort) {
        return "//:" + rmiPort + "/";
    }
}
