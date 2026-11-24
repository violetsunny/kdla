package top.kdla.framework.supplement.trdcloud.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.kdla.framework.supplement.trdcloud.utils.JwtUtil;

import java.io.IOException;

/**
 * trd 接口 blade-auth 准入过滤器：缺头、验签失败、过期、密钥未配置一律 401，fail-closed。
 * 控制器内联的 blade-auth 守卫仅用于填充 createUser/updateUser，准入由本过滤器统一承担。
 */
@Slf4j
public class TrdBladeAuthFilter implements Filter {

    private static final String UNAUTHORIZED_BODY =
            "{\"success\":false,\"code\":\"401\",\"msg\":\"认证失败或未登录\",\"message\":\"认证失败或未登录\"}";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String auth = req.getHeader("blade-auth");
        try {
            JwtUtil.getInfoFromToken(auth, "account");
        } catch (Exception e) {
            log.warn("trd 接口认证失败 uri={} cause={}", req.getRequestURI(), e.getMessage());
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(UNAUTHORIZED_BODY);
            return;
        }
        chain.doFilter(request, response);
    }
}