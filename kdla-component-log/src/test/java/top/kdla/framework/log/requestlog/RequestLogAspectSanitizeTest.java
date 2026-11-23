package top.kdla.framework.log.requestlog;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：RequestLogAspect此前把全部Header原值、完整响应无脱敏无截断打印。
 * 修复后：黑名单Header（authorization等）值替换为***；响应体超2KB截断。
 */
class RequestLogAspectSanitizeTest {

    public static class DemoController {
        public String hello() {
            return "ok";
        }
    }

    private Logger aspectLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        aspectLogger = (Logger) LoggerFactory.getLogger(RequestLogAspect.class);
        appender = new ListAppender<>();
        appender.start();
        aspectLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        aspectLogger.detachAppender(appender);
        RequestContextHolder.resetRequestAttributes();
    }

    private ProceedingJoinPoint mockPjp(Object result) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = DemoController.class.getMethod("hello");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(signature.getMethod()).thenReturn(method);
        when(pjp.proceed()).thenReturn(result);
        return pjp;
    }

    private String capturedLogs() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sensitiveHeaders_masked_nonSensitiveKept() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.addHeader("Authorization", "Bearer secret-token-value");
        request.addHeader("Cookie", "SESSION=abc123");
        request.addHeader("X-App-Name", "demo-app");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        new RequestLogAspect().aroundApi(mockPjp("ok"));

        String logged = capturedLogs();
        assertTrue(logged.contains("Request Start"), "必须打印请求日志");
        assertFalse(logged.contains("secret-token-value"), "Authorization头原值不得落日志");
        assertFalse(logged.contains("SESSION=abc123"), "Cookie头原值不得落日志");
        assertTrue(logged.contains("***"), "黑名单Header值必须替换为***");
        assertTrue(logged.contains("demo-app"), "非敏感Header保持原值");
    }

    @Test
    @SuppressWarnings("unchecked")
    void hugeResponse_truncatedInLog() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/big");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            huge.append('z');
        }
        new RequestLogAspect().aroundApi(mockPjp(huge.toString()));

        String logged = capturedLogs();
        assertTrue(logged.contains("...(truncated, total="), "超长响应必须截断并带total标记");
        assertFalse(logged.contains(huge.toString()), "完整3000字符响应不得整体落日志");
    }
}
