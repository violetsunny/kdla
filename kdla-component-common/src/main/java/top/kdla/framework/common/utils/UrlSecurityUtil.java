package top.kdla.framework.common.utils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * SSRF 首跳防护：仅放行 http/https 且主机解析结果全部为公网地址。
 * 下游 URL 预览 / webhook / 图片抓取等用户可控 URL 场景，应在发起请求前调用。
 */
public class UrlSecurityUtil {

    private UrlSecurityUtil() {
    }

    /**
     * ponytail: 仅校验首跳。DNS rebinding（校验后重新解析）与 302 重定向可绕过；
     * 下游须同时关闭/限制重定向（Vertx HttpClientOptions 默认 maxRedirects=16、Apache HttpClient5 默认跟随），
     * 并对每一跳重新校验，或改用固定出口代理。升级路径：连接期绑定已校验 IP。
     */
    public static boolean isPublicHttpUrl(String url) {
        if (url == null || url.trim().isEmpty() || url.indexOf('\\') >= 0) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return false;
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return false;
        }
        for (InetAddress address : addresses) {
            if (!isPublic(address)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPublic(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        // IPv6 唯一本地地址 fc00::/7 不被 isSiteLocalAddress 覆盖
        return bytes.length != 16 || (bytes[0] & 0xfe) != 0xfc;
    }
}
