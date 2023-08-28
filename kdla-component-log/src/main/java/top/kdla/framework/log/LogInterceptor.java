package top.kdla.framework.log;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import top.kdla.framework.common.constants.CommonConstants;
import top.kdla.framework.common.help.SelfSnowflakeGeneratorHelp;
import top.kdla.framework.common.utils.JacksonUtil;
import top.kdla.framework.common.utils.TimeUtil;
import top.kdla.framework.log.webfilter.KdlaHttpServletRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 日志拦截器
 *
 * @author kll
 * @data 2021/7/15
 */
@Slf4j
public class LogInterceptor implements AsyncHandlerInterceptor {

    private static final ThreadLocal<Long> BEGIN_TIMESTAMP = new ThreadLocal<>();

    private void printHttpRequestHeaderInfo(final HttpServletRequest request) {
        try {
            KdlaHttpServletRequestWrapper requestWrapper = new KdlaHttpServletRequestWrapper(request);
            // 打印请求信息
            log.info("### uri：{}, requestParam: {}, requestBody:{} ", requestWrapper.getRequestURI(), JacksonUtil.toJson(requestWrapper.getParameterMap()), requestWrapper.getBody());
        } catch (Exception e) {
            log.info("打印请求头信息异常:{}", e.getMessage());
        }
    }


    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        //日志跟踪标识traceId
        if (!StringUtils.hasText(MDC.get(CommonConstants.LOG_TRACE_ID))) {
            JSONObject traceIdInfo = new JSONObject();
            traceIdInfo.put(CommonConstants.LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
            MDC.put(CommonConstants.LOG_TRACE_ID, traceIdInfo.toJSONString());
        }
        LogTraceHolder.set(Boolean.TRUE);
        BEGIN_TIMESTAMP.set(TimeUtil.now());
        printHttpRequestHeaderInfo(request);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response,
                                final Object handler, final Exception ex) throws Exception {
        Long executionTime = TimeUtil.now() - BEGIN_TIMESTAMP.get();
        log.info(String.format("### uri: %s, remote-addr: %s, execution: %d", request.getRequestURI(), request.getRemoteAddr(), executionTime));
        if (LogTraceHolder.get()) {
            MDC.remove(CommonConstants.LOG_TRACE_ID);
        }
        LogTraceHolder.remove();
        BEGIN_TIMESTAMP.remove();
    }

}
