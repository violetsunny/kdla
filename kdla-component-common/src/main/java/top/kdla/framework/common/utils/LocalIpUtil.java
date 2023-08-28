package top.kdla.framework.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

/**
 * ip
 * @author kll
 * @version $Id: LocalIpUtils.java $
 */
@Slf4j
public class LocalIpUtil {

    public static final String DEFAULT_IP = "127.0.0.1";

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
        try {
            String xIp = request.getHeader("X-Real-IP");
            String xFor = request.getHeader("X-Forwarded-For");
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(xFor) && !UNKNOWN.equalsIgnoreCase(xFor)) {
                //多次反向代理后会有多个ip值，第一个ip才是真实ip
                int index = xFor.indexOf(",");
                if (index != -1) {
                    return xFor.substring(0, index);
                } else {
                    return xFor;
                }
            }
            xFor = xIp;
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(xFor) && !UNKNOWN.equalsIgnoreCase(xFor)) {
                return xFor;
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {
                xFor = request.getHeader("Proxy-Client-IP");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {
                xFor = request.getHeader("WL-Proxy-Client-IP");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {
                xFor = request.getHeader("HTTP_CLIENT_IP");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {
                xFor = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StringUtils.isBlank(xFor) || UNKNOWN.equalsIgnoreCase(xFor)) {
                xFor = request.getRemoteAddr();
            }

            if ("localhost".equalsIgnoreCase(xFor) || "127.0.0.1".equalsIgnoreCase(xFor) || "0:0:0:0:0:0:0:1".equalsIgnoreCase(xFor)) {
                return getLocalIp4Address();
            }
            return xFor;
        } catch (Exception e) {
            log.error("get remote ip error!", e);
            return "x.0.0.1";
        }
    }

    /**
     * 获取本机所有网卡信息   得到所有IP信息
     *
     * @return Inet4Address>
     */
    private static List<Inet4Address> getLocalIp4AddressFromNetworkInterface() throws SocketException {
        List<Inet4Address> addresses = new ArrayList<>(1);

        // 所有网络接口信息
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        if (ObjectUtils.isEmpty(networkInterfaces)) {
            return addresses;
        }
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            //滤回环网卡、点对点网卡、非活动网卡、虚拟网卡并要求网卡名字是eth或ens开头
            if (!isValidInterface(networkInterface)) {
                continue;
            }

            // 所有网络接口的IP地址信息
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                // 判断是否是IPv4，并且内网地址并过滤回环地址.
                if (isValidAddress(inetAddress)) {
                    addresses.add((Inet4Address) inetAddress);
                }
            }
        }
        return addresses;
    }

    /**
     * 过滤回环网卡、点对点网卡、非活动网卡、虚拟网卡并要求网卡名字是eth或ens开头
     *
     * @param ni 网卡
     * @return 如果满足要求则true，否则false
     */
    private static boolean isValidInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual()
                && (ni.getName().startsWith("eth") || ni.getName().startsWith("ens"));
    }

    /**
     * 判断是否是IPv4，并且内网地址并过滤回环地址.
     */
    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress();
    }

    /**
     * 通过Socket 唯一确定一个IP
     * 当有多个网卡的时候，使用这种方式一般都可以得到想要的IP。甚至不要求外网地址8.8.8.8是可连通的
     *
     * @return Inet4Address>
     */
    private static Optional<Inet4Address> getIpBySocket() throws SocketException {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            if (socket.getLocalAddress() instanceof Inet4Address) {
                return Optional.of((Inet4Address) socket.getLocalAddress());
            }
        } catch (UnknownHostException networkInterfaces) {
            throw new RuntimeException(networkInterfaces);
        }
        return Optional.empty();
    }

    /**
     * 获取本地IPv4地址
     *
     * @return Inet4Address>
     */
    public static String getLocalIp4Address() throws SocketException {
        final List<Inet4Address> inet4Addresses = getLocalIp4AddressFromNetworkInterface();
        if (inet4Addresses.size() != 1) {
            final Optional<Inet4Address> ipBySocketOpt = getIpBySocket();
            return ipBySocketOpt.map(Inet4Address::getHostAddress).orElseGet(() -> inet4Addresses.isEmpty() ? DEFAULT_IP : inet4Addresses.get(0).getHostAddress());
        }
        return inet4Addresses.get(0).getHostAddress();
    }
}
