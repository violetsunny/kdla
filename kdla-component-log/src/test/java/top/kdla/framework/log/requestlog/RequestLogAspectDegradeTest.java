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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归：兜底try-catch被删致解析异常炸业务请求；空List get(0)越界；非Web上下文NPE。
 * 另覆盖Spring6.1删LVT参数名发现器：未开-parameters时日志参数名回退读字节码。
 */
class RequestLogAspectDegradeTest {

    public static class DemoController {
        public String hello() {
            return "ok";
        }

        public String list(List<String> items) {
            return "ok";
        }

        public String named(String userName) {
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

    private ProceedingJoinPoint mockPjp(String methodName, Class<?>[] paramTypes, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = DemoController.class.getMethod(methodName, paramTypes);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(args);
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
    void nonWebContext_stillProceeds() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        ProceedingJoinPoint pjp = mockPjp("hello", new Class<?>[0], new Object[0], "ok");

        // 修复前：requireNonNull(request)抛NPE炸掉业务方法
        assertEquals("ok", new RequestLogAspect().aroundApi(pjp), "非Web上下文必须降级跳过日志照常执行业务");
    }

    @Test
    void emptyListArg_stillProceeds() throws Throwable {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/list")));
        ProceedingJoinPoint pjp = mockPjp("list", new Class<?>[]{List.class}, new Object[]{new ArrayList<String>()}, "ok");

        // 修复前：空List get(0)抛IndexOutOfBoundsException
        assertEquals("ok", new RequestLogAspect().aroundApi(pjp), "空List参数不得越界");
    }

    @Test
    void parameterName_lvtFallback_usedAsLogKey() throws Throwable {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("GET", "/api/named")));
        ProceedingJoinPoint pjp = mockPjp("named", new Class<?>[]{String.class}, new Object[]{"alice"}, "ok");

        new RequestLogAspect().aroundApi(pjp);

        String logged = capturedLogs();
        // 宿主未开-parameters时Spring6.1反射拿不到参数名，修复前日志key为null
        assertTrue(logged.contains("userName"), "日志参数名必须回退读字节码LVT，不得为null");
        assertTrue(logged.contains("alice"), "参数值必须照常落日志");
    }
}