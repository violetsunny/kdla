package top.kdla.framework.common.utils;

import java.net.*;
import java.util.Enumeration;

/**
 * ip
 * @author kll
 * @version $Id: LocalIpUtils.java $
 */
public class LocalIpUtils {
    private static String LocalIpAddress;

    public static String getLocalIp() {
        if (null != LocalIpAddress && LocalIpAddress.trim().length() != 0) {
            return LocalIpAddress;
        }

        String result = "";
        boolean hasEx = false;
        try {
            result = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            hasEx = true;
        }

        if (hasEx) {
            try {
                Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
                InetAddress ip = null;
                while (allNetInterfaces.hasMoreElements()) {
                    NetworkInterface netInterface = (NetworkInterface)allNetInterfaces.nextElement();
                    System.out.println(netInterface.getName());
                    Enumeration addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = (InetAddress)addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            System.out.println("本机的IP = " + ip.getHostAddress());
                            result = ip.getHostAddress();
                        }
                    }
                }
            } catch (SocketException se) {
                hasEx = true;
            }
        }

        LocalIpAddress = result;
        return result;
    }
}
