package top.kdla.framework.log.catchlog;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import top.kdla.framework.log.LogTraceHolder;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * 回归测试：CatchLogAspect的Invoke Finished日志此前把完整请求参数与响应无截断打印，
 * 大报文会刷爆日志。修复后超过2KB截断并追加"...(truncated, total=N)"标记。
 */
class CatchLogAspectLogTruncateTest {

    static class StringService {
        public String doWork() {
            return "ok";
        }
    }

    private Logger aspectLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        aspectLogger = (Logger) LoggerFactory.getLogger(CatchLogAspect.class);
        appender = new ListAppender<>();
        appender.start();
        aspectLogger.addAppender(appender);
        org.slf4j.MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @AfterEach
    void tearDown() {
        aspectLogger.detachAppender(appender);
        org.slf4j.MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @Test
    @SuppressWarnings("unchecked")
    void hugeArgsAndResponse_truncatedInLog() throws Throwable {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            huge.append('y');
        }

        CatchLogAspect aspect = new CatchLogAspect();
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = StringService.class.getMethod("doWork");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{huge.toString()});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getReturnType()).thenReturn((Class) String.class);
        when(pjp.proceed()).thenAnswer(invocation -> {
            aspect.doBefore(pjp);
            return huge.toString();
        });

        aspect.around(pjp);

        String logged = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertTrue(logged.contains("Invoke Finished"), "必须打印Invoke Finished日志");
        assertTrue(logged.contains("...(truncated, total="), "超长参数/响应必须截断并带total标记");
        assertFalse(logged.contains(huge.toString()), "完整3000字符报文不得整体落日志");
    }
}
