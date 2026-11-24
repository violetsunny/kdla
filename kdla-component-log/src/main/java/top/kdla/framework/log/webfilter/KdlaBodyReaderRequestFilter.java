package top.kdla.framework.log.webfilter;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import top.kdla.framework.common.help.KdlaStringHelp;
import top.kdla.framework.common.help.SelfSnowflakeGeneratorHelp;
import top.kdla.framework.common.utils.LogSanitizeUtil;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * kdlaBodyReaderRequest过滤器,解决Request的Body只能读取一次的解决方法
 *
 * @author kll
 * @date 2021/8/9
 */
@WebFilter(urlPatterns = "/*", filterName = "KdlaBodyReaderRequestFilter")
public class KdlaBodyReaderRequestFilter implements Filter {

    /**
     * 排除链接
     */
    @Value("${kdla.repeatable.filter.url.excludes:/config/**,/ping}")
    private List<String> excludes;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_DELETE = "DELETE";


    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        // 获取trace
        if (!StringUtils.hasText(MDC.get(LOG_TRACE_ID))) {
            String traceId = request.getHeader(LOG_TRACE_ID);
            // 外来traceId须过格式白名单，不合法（含CRLF/超长）自生成，防伪造日志行
            if (LogSanitizeUtil.validTraceId(traceId)) {
                MDC.put(LOG_TRACE_ID, traceId);
            } else {
                // 自定义一个
                MDC.put(LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
            }
        }

        try {
            if (handleExcludeURL(request, response)) {
                filterChain.doFilter(request, response);
                return;
            }
            KdlaHttpServletRequestWrapper requestWrapper = null;
            if (KdlaStringHelp.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
                requestWrapper = new KdlaHttpServletRequestWrapper(request);
            }
            if (Objects.isNull(requestWrapper)) {
                filterChain.doFilter(request, response);
            } else {
                filterChain.doFilter(requestWrapper, response);
            }
        } finally {
            MDC.remove(LOG_TRACE_ID);
        }
    }

    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getServletPath();
        String method = request.getMethod();
        // GET DELETE 不过滤（此前漏判GET，仅排除了DELETE，与注释语义不符）
        if (excludes == null || excludes.isEmpty() || method == null
                || method.matches(METHOD_GET) || method.matches(METHOD_DELETE)) {
            return true;
        }
        return KdlaStringHelp.matches(url, excludes);
    }
}
