package top.kdla.framework.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import top.kdla.framework.log.webfilter.KdlaHttpServletRequestWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * 回归：preHandle曾直接new wrapper消费原始输入流致下游@RequestBody空body；
 * afterCompletion曾直接拆箱BEGIN_TIMESTAMP.get()，preHandle未执行路径NPE。
 */
class LogInterceptorBodyAndTimingTest {

    private LogInterceptor interceptor;
    private Logger interceptorLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        interceptor = new LogInterceptor();
        interceptorLogger = (Logger) LoggerFactory.getLogger(LogInterceptor.class);
        appender = new ListAppender<>();
        appender.start();
        interceptorLogger.addAppender(appender);
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @AfterEach
    void tearDown() {
        interceptorLogger.detachAppender(appender);
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    private String capturedLogs() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    @Test
    void preHandle_plainRequest_doesNotConsumeInputStream() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/x");
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());

        interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

        // 修复前：new KdlaHttpServletRequestWrapper(request)读掉原始流，下游controller拿到空body
        verify(request, never()).getInputStream();
        assertTrue(capturedLogs().contains("<body未缓存>"), "未缓存body应打印占位符，不得消费原始流");
    }

    @Test
    void preHandle_wrappedRequest_logsCachedBody() {
        MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/x");
        raw.setContent("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        KdlaHttpServletRequestWrapper wrapper = new KdlaHttpServletRequestWrapper(raw);

        interceptor.preHandle(wrapper, mock(HttpServletResponse.class), new Object());

        assertTrue(capturedLogs().contains("{\"a\":1}"), "已缓存body必须原样打印");
    }

    @Test
    void afterCompletion_withoutPreHandle_doesNotThrow() throws Exception {
        Field field = LogInterceptor.class.getDeclaredField("BEGIN_TIMESTAMP");
        field.setAccessible(true);
        ((ThreadLocal<?>) field.get(null)).remove();

        HttpServletRequest request = mock(HttpServletRequest.class);
        // 修复前：BEGIN_TIMESTAMP.get()为null，拆箱NPE
        assertDoesNotThrow(() -> interceptor.afterCompletion(request, mock(HttpServletResponse.class), new Object(), null),
                "preHandle未执行时afterCompletion不得NPE");
    }
}