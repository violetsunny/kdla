package top.kdla.framework.common.aspect.watch;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：旧实现用 getTarget().getClass().getMethod(...) 反射重取注解，
 * 可能抛 NoSuchMethodException，且 getAnnotation 结果未判空（stopWatchWrapper.logHead() 直接 NPE）。
 * 修复后改用 signature.getMethod() + AopUtils.getMostSpecificMethod + AnnotationUtils.findAnnotation，
 * 注解为 null 时跳过计时直接执行业务方法。
 */
class StopWatchWrapperAspectTest {

    static class DemoService {
        @StopWatchWrapper(logHead = "[test]", msg = "demo")
        public String run() {
            return "done";
        }

        public String plain() {
            return "plain-ok";
        }
    }

    private static StopWatchWrapperAspect newAspect() {
        StopWatchWrapperAspect aspect = new StopWatchWrapperAspect();
        ReflectionTestUtils.setField(aspect, "errorTimeOut", 2000);
        ReflectionTestUtils.setField(aspect, "warnTimeOut", 200);
        return aspect;
    }

    @Test
    void annotatedMethod_stillTimedAndReturnsValue() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new DemoService());
        factory.addAspect(newAspect());
        DemoService proxy = factory.getProxy();

        assertEquals("done", proxy.run(), "有注解的方法必须正常计时并返回业务结果");
    }

    @Test
    void annotationMissing_skipsTimingInsteadOfNpe() throws Throwable {
        StopWatchWrapperAspect aspect = newAspect();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method plainMethod = DemoService.class.getMethod("plain");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(plainMethod);
        when(signature.getName()).thenReturn("plain");
        when(joinPoint.getTarget()).thenReturn(new DemoService());
        when(joinPoint.proceed()).thenReturn("plain-ok");

        assertEquals("plain-ok", aspect.around(joinPoint), "注解缺失时必须跳过计时直接执行业务方法，不能 NPE");
    }
}
