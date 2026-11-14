package top.kdla.framework.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * 回归测试：LogInterceptor.preHandle 此前把 {"traceId":"xxx"} 整个JSON串放进MDC，
 * 与 KdlaBodyReaderRequestFilter/CatchLogAspect 放入的裸traceId格式不一致，日志前缀出现双重JSON。
 * 修复后：MDC中存放裸traceId字符串。
 */
class LogInterceptorTraceIdTest {

    private LogInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new LogInterceptor();
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @AfterEach
    void tearDown() {
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @Test
    void preHandle_putsRawTraceId_notJsonString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        interceptor.preHandle(request, response, new Object());

        String traceId = MDC.get(LOG_TRACE_ID);
        assertNotNull(traceId, "preHandle后MDC必须有traceId");
        // 修复前：traceId是 {"traceId":"ac13e001.xxx"} 形式的JSON串
        assertFalse(traceId.startsWith("{"), "MDC必须放裸traceId字符串，不得是JSON串");
        assertFalse(traceId.contains(LOG_TRACE_ID + "\":"), "MDC值不得包含JSON键名");
    }

    @Test
    void preHandle_existingTraceId_notOverwritten() {
        MDC.put(LOG_TRACE_ID, "existing-trace");
        interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object());
        assertNotNull(MDC.get(LOG_TRACE_ID));
        assertFalse(MDC.get(LOG_TRACE_ID).startsWith("{"));
    }

    @Test
    void afterCompletion_cleansUpMdcAndHolder() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        interceptor.preHandle(request, response, new Object());
        assertNotNull(MDC.get(LOG_TRACE_ID));

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(MDC.get(LOG_TRACE_ID), "afterCompletion必须清理本拦截器放入的traceId");
    }
}
