package top.kdla.framework.catchlog.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import top.kdla.framework.catchlog.filter.wrapper.KdlaHttpServletRequestWrapper;
import top.kdla.framework.common.utils.KdlaStringUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * kdlaBodyReaderRequest过滤器,解决Request的Body只能读取一次的解决方法
 *
 * @author kll
 * @date 2021/8/9
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "KdlaBodyReaderRequestFilter")
public class KdlaBodyReaderRequestFilter implements Filter {

    /**
     * 排除链接
     */
    @Value("${kdla.repeatable.filter.url.excludes:/rdfa-timer/**,/config/**,/ping}")
    private List<String> excludes;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_DELETE = "DELETE";


    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;
        if (handleExcludeURL(request, response)) {
            filterChain.doFilter(request, response);
            return;
        }
        KdlaHttpServletRequestWrapper requestWrapper = null;
        if (request instanceof HttpServletRequest && KdlaStringUtil
            .startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
            requestWrapper = new KdlaHttpServletRequestWrapper(request);
        }
        if (Objects.isNull(requestWrapper)) {
            filterChain.doFilter(request, response);
        } else {
            filterChain.doFilter(requestWrapper, response);
        }
    }

    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getServletPath();
        String method = request.getMethod();
        // GET DELETE 不过滤
        if (method == null || method.matches(METHOD_DELETE)) {
            return true;
        }
        return KdlaStringUtil.matches(url, excludes);
    }
}
