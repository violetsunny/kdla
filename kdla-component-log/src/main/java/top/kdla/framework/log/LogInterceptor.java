package top.kdla.framework.log;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import top.kdla.framework.common.constants.CommonConstants;
import top.kdla.framework.common.help.DateHelp;
import top.kdla.framework.common.help.SelfSnowflakeGeneratorHelp;
import top.kdla.framework.common.utils.JacksonUtil;
import top.kdla.framework.log.sanitize.LogSanitizeUtil;
import top.kdla.framework.log.webfilter.KdlaHttpServletRequestWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 日志拦截器
 *
 * @author kll
 * @data 2021/7/15
 */
@Slf4j
public class LogInterceptor implements AsyncHandlerInterceptor {

    private static final InheritableThreadLocal<Long> BEGIN_TIMESTAMP = new InheritableThreadLocal<>();

    private void printHttpRequestHeaderInfo(final HttpServletRequest request) {
        try {
            // 仅当请求已被KdlaBodyReaderRequestFilter缓存时才打印body；
            // 直接new wrapper会消费原始输入流且未替换下游request，导致controller读到空body
            String body = request instanceof KdlaHttpServletRequestWrapper
                    ? ((KdlaHttpServletRequestWrapper) request).getBody() : null;
            // 打印请求信息
            if (log.isInfoEnabled()) {
                log.info("### uri：{}, requestParam: {}, requestBody:{} ", request.getRequestURI(), JacksonUtil.toJson(request.getParameterMap()), body == null ? "<body未缓存>" : LogSanitizeUtil.truncate(body));
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("打印请求头信息异常:{}", e.getMessage());
            }
        }
    }


    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        //日志跟踪标识traceId
        if (!StringUtils.hasText(MDC.get(CommonConstants.LOG_TRACE_ID))) {
            // 放裸traceId字符串，与KdlaBodyReaderRequestFilter/CatchLogAspect格式一致（此前误放JSON串导致日志前缀格式不统一）
            MDC.put(CommonConstants.LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
        }
        LogTraceHolder.set(Boolean.TRUE);
        BEGIN_TIMESTAMP.set(DateHelp.now());
        printHttpRequestHeaderInfo(request);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response,
                                final Object handler, final Exception ex) throws Exception {
        // 异步dispatch/预检失败等preHandle未执行的路径下get()为null，直接拆箱会NPE；-1表示未计时
        Long begin = BEGIN_TIMESTAMP.get();
        long executionTime = begin == null ? -1L : DateHelp.now() - begin;
        if (log.isInfoEnabled()) {
            log.info("### uri: {}, remote-addr: {}, execution: {}", request.getRequestURI(), request.getRemoteAddr(), executionTime);
        }
        if (LogTraceHolder.get()) {
            MDC.remove(CommonConstants.LOG_TRACE_ID);
        }
        LogTraceHolder.remove();
        BEGIN_TIMESTAMP.remove();
    }

}
