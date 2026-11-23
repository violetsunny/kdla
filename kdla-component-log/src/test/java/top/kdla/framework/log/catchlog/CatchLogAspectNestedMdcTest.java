package top.kdla.framework.log.catchlog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.kdla.framework.dto.Response;
import top.kdla.framework.log.LogTraceHolder;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * 回归测试：嵌套@CatchAndLog时，内层around的finally无条件MDC.remove，
 * 把外层doBefore放入的traceId清掉，外层方法后续日志全部丢失traceId。
 * 修复后：around入口记录"traceId是否由本层放入"（入口MDC为空即本层放入），
 * finally仅本层放入才清理（保存-恢复模式，与MdcAspect一致）。
 */
class CatchLogAspectNestedMdcTest {

    static class DemoService {
        public Response doWork() {
            return Response.buildSuccess();
        }
    }

    private CatchLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CatchLogAspect();
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @AfterEach
    void tearDown() {
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    private ProceedingJoinPoint mockPjp() throws Exception {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = DemoService.class.getMethod("doWork");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getReturnType()).thenReturn((Class) Response.class);
        return pjp;
    }

    @Test
    @SuppressWarnings("unchecked")
    void nestedCatchAndLog_innerFinally_keepsOuterTraceId() throws Throwable {
        ProceedingJoinPoint outerPjp = mockPjp();
        ProceedingJoinPoint innerPjp = mockPjp();
        AtomicReference<String> outerTraceId = new AtomicReference<>();

        // 模拟Spring AOP真实时序：@Before在around的proceed()内触发
        when(innerPjp.proceed()).thenAnswer(invocation -> {
            aspect.doBefore(innerPjp);
            return Response.buildSuccess();
        });
        when(outerPjp.proceed()).thenAnswer(invocation -> {
            aspect.doBefore(outerPjp);
            outerTraceId.set(MDC.get(LOG_TRACE_ID));
            Object result = aspect.around(innerPjp);
            // 修复前：内层finally已把外层traceId清掉，此处为null
            assertNotNull(MDC.get(LOG_TRACE_ID), "内层切面结束后外层traceId必须仍在MDC中");
            assertEquals(outerTraceId.get(), MDC.get(LOG_TRACE_ID), "内层不得覆盖/清除外层traceId");
            return result;
        });

        aspect.around(outerPjp);

        assertNotNull(outerTraceId.get(), "外层doBefore必须已放入traceId");
        assertNull(MDC.get(LOG_TRACE_ID), "最外层结束后本层放入的traceId必须被清理");
    }

    @Test
    @SuppressWarnings("unchecked")
    void singleLayer_stillCleansUpOwnTraceId() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp();
        when(pjp.proceed()).thenAnswer(invocation -> {
            aspect.doBefore(pjp);
            assertNotNull(MDC.get(LOG_TRACE_ID));
            return Response.buildSuccess();
        });

        aspect.around(pjp);

        assertNull(MDC.get(LOG_TRACE_ID), "单层切面结束后必须清理本层放入的traceId");
    }

    @Test
    @SuppressWarnings("unchecked")
    void webContext_traceHolderTrue_aspectDoesNotRemove() throws Throwable {
        // 模拟Web场景：Filter/Interceptor已放入traceId且LogTraceHolder=true，切面不负责清理
        MDC.put(LOG_TRACE_ID, "web-trace-1");
        LogTraceHolder.set(Boolean.TRUE);

        ProceedingJoinPoint pjp = mockPjp();
        when(pjp.proceed()).thenAnswer(invocation -> {
            aspect.doBefore(pjp);
            return Response.buildSuccess();
        });

        aspect.around(pjp);

        assertEquals("web-trace-1", MDC.get(LOG_TRACE_ID), "Web上下文由Filter/Interceptor管理traceId，切面不得清除");
    }
}
