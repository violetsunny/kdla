package top.kdla.framework.common.help;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：旧实现反射 java.lang.reflect.Proxy.h + setAccessible，
 * JDK17+ 强封装下抛 InaccessibleObjectException。修复后经 Advised.getTargetSource().getTarget() 取目标。
 */
class AopTargetHelpTest {

    interface Demo {
        String run();
    }

    static class DemoImpl implements Demo {
        @Override
        public String run() {
            return "ok";
        }
    }

    static class Plain {
        public String run() {
            return "plain";
        }
    }

    private static final MethodInterceptor PASS_THROUGH = invocation -> invocation.proceed();

    @Test
    void jdkDynamicProxy_returnsRealTarget() {
        DemoImpl target = new DemoImpl();
        ProxyFactory factory = new ProxyFactory(target);
        factory.setInterfaces(Demo.class);
        factory.addAdvice(PASS_THROUGH);
        Demo proxy = (Demo) factory.getProxy();
        assertTrue(AopUtils.isJdkDynamicProxy(proxy));
        assertSame(target, AopTargetHelp.getTarget(proxy), "JDK 动态代理必须取回真实目标对象");
    }

    @Test
    void cglibProxy_returnsRealTarget() {
        Plain target = new Plain();
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(PASS_THROUGH);
        Plain proxy = (Plain) factory.getProxy();
        assertTrue(AopUtils.isCglibProxy(proxy));
        assertSame(target, AopTargetHelp.getTarget(proxy), "CGLIB 代理必须取回真实目标对象");
    }

    @Test
    void nonProxy_returnsAsIs() {
        Plain plain = new Plain();
        assertSame(plain, AopTargetHelp.getTarget(plain), "非代理对象原样返回");
    }
}