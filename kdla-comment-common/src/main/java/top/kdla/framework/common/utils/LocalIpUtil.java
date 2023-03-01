package top.kdla.framework.common.utils;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.Enumeration;

/**
 * ip
 * @author kll
 * @version $Id: LocalIpUtils.java $
 */
public class LocalIpUtil {
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

    private final static String UNKNOWN = "unknown";

    /**
     * 获取IP地址
     *
     * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
     * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = null;
        try {
            ip = request.getHeader("x-forwarded-for");
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StringUtils.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {

        }

        return ip;
    }
}
