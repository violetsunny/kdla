package top.kdla.framework.catchlog;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import top.kdla.framework.catchlog.filter.wrapper.KdlaHttpServletRequestWrapper;
import top.kdla.framework.common.constants.CommonConstants;
import top.kdla.framework.common.utils.JacksonUtil;
import top.kdla.framework.common.utils.RequestUtils;
import top.kdla.framework.common.utils.TimeUtilSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 日志拦截器
 * @author vincent.li
 * @data 2021/7/15
 */
public class LogInterceptor extends HandlerInterceptorAdapter {

    private TimeUtilSupport timeUtilSupport;
    private ThreadLocal<Long> beginTimestamp;
    private Logger logger;

    public LogInterceptor(final TimeUtilSupport timeUtilSupport, final ThreadLocal<Long> beginTimestamp,
                          final Logger logger) {
        this.timeUtilSupport = timeUtilSupport;
        this.beginTimestamp = beginTimestamp;
        this.logger = logger;
    }

    private void printHttpRequestHeaderInfo(final HttpServletRequest request){
        try {
            KdlaHttpServletRequestWrapper requestWrapper = new KdlaHttpServletRequestWrapper(request);
            // 打印请求信息
            logger.info("### uri：{}, requestParam: {}, requestBody:{} ", requestWrapper.getRequestURI(), JacksonUtil.toJson(requestWrapper.getParameterMap()), requestWrapper.getBody());

        } catch (Exception e) {
            logger.info("打印请求头信息异常:{}", e.getMessage());
        }
    }


    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
       //日志跟踪标识traceId
        if (StringUtils.isEmpty(MDC.get(CommonConstants.LOG_TRACE_ID))) {
            JSONObject traceIdInfo = new JSONObject();
            traceIdInfo.put(CommonConstants.LOG_TRACE_ID, RequestUtils.getTraceId());
            MDC.put(CommonConstants.LOG_TRACE_ID, traceIdInfo.toJSONString());
        }
        LogTraceHolder.set(Boolean.TRUE);
        beginTimestamp.set(timeUtilSupport.now());
        printHttpRequestHeaderInfo(request);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response,
        final Object handler, final Exception ex) throws Exception {
        Long executionTime = timeUtilSupport.now() - beginTimestamp.get();
        logger.info(String.format("### uri: %s, remote-addr: %s, execution: %d",
            request.getRequestURI(), request.getRemoteAddr(), executionTime));
        if (LogTraceHolder.get()) {
            MDC.remove(CommonConstants.LOG_TRACE_ID);
        }
        LogTraceHolder.remove();

    }

}
