package top.kdla.framework.supplement.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * HttpContextUtils
 **/
@Slf4j
public class HttpContextHelp {

    public static HttpServletRequest getHttpServletRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static String getDomain() {
        HttpServletRequest request = getHttpServletRequest();
        StringBuffer url = request.getRequestURL();
        return url.delete(url.length() - request.getRequestURI().length(), url.length()).toString();
    }

    public static String getOrigin() {
        HttpServletRequest request = getHttpServletRequest();
        return request.getHeader("Origin");
    }

    private static void trustAllHttpsCertificates() throws Exception {
        if (log.isWarnEnabled()) {
            log.warn("WARNING: trustAllHttpsCertificates invoked — all SSL certificates will be accepted. This is insecure and should NOT be used in production.");
        }
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    static class miTM implements TrustManager, X509TrustManager {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }
    }

    /**
     * 忽略HTTPS请求的SSL证书，必须在openConnection之前调用
     *
     * @throws Exception
     * @deprecated 本方法修改 {@link HttpsURLConnection} 的 JVM 级默认 SSLSocketFactory 与
     *             HostnameVerifier，一次调用使宿主进程内所有 HTTPS 连接可被中间人攻击。
     *             仅为兼容保留；请改用连接级配置 {@code kdla.http.ssl.trust-all=true}
     *             （VertxHttpConfigure，仅作用于本客户端且默认关闭）。
     */
    @Deprecated
    public static void ignoreSSL() throws Exception {
        HostnameVerifier hv = (urlHostName, session) -> {
            if (log.isWarnEnabled()) {
                log.warn("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
            }
            return true;
        };
        trustAllHttpsCertificates();
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        log.warn("ignoreSSL已修改JVM全局SSL默认配置，宿主进程所有HTTPS连接均不再校验证书；请迁移到kdla.http.ssl.trust-all连接级配置");
    }

    /**
     * 支持跨域
     *
     * @param request
     * @param response
     * @deprecated 反射任意 Origin 且 Allow-Credentials:true，等价于对任意站点开放带凭据跨域。
     *             仅为兼容保留；生产请使用 {@code kdla.config.cors.enabled=true} 的
     *             CustomCorsConfig 并配置 origin 白名单。
     */
    @Deprecated
    public static void buildCors(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (StringUtils.isBlank(origin)) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "false");
        } else {
            if (log.isWarnEnabled()) {
                log.warn("buildCors reflecting arbitrary Origin '{}' with credentials — configure an origin whitelist for production use.", origin);
            }
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, X-Real-IP, X-Forwarded-For, d-uuid, User-Agent, x-zd-cs, Proxy-Client-IP, HTTP_CLIENT_IP, HTTP_X_FORWARDED_FOR");
    }
}
