package top.kdla.framework.supplement.http;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HttpContextUtils
 **/
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

    /**
     * @deprecated 已禁用：一次调用使宿主进程所有 HTTPS 连接可被中间人攻击。
     *             请改用连接级配置 {@code kdla.http.ssl.trust-all}（VertxHttpConfigure，仅作用于本客户端且默认关闭）。
     */
    @Deprecated
    public static void ignoreSSL() {
        throw new UnsupportedOperationException("ignoreSSL已禁用：全局trust-all可被中间人攻击，请改用kdla.http.ssl.trust-all连接级配置");
    }

    /**
     * @deprecated 已禁用：反射任意 Origin 且 Allow-Credentials:true，等价于对任意站点开放带凭据跨域。
     *             请启用 {@code kdla.config.cors.enabled} 并配置 {@code kdla.config.cors.allowed-origins}
     *             白名单（CustomCorsConfig），或自定义 CorsFilter Bean。
     */
    @Deprecated
    public static void buildCors(HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("buildCors已禁用：任意Origin带凭据跨域可被窃取响应，请改用CustomCorsConfig origin白名单");
    }
}
