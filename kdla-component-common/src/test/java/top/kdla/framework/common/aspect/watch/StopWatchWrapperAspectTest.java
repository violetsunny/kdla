package top.kdla.framework.common.aspect.watch;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：
 * 1. 有注解方法正常计时返回；无注解方法不被切面拦截（2.x 用 pointcut 参数绑定注解，绑定即非空）。
 * 2. 旧实现 cost>error || cost>warn 等价 cost>warn，但文案恒标 error 阈值→告警误分级。
 *    修复后分级 if-else：仅超 warn 阈值时文案必须标 warn 阈值。
 */
class StopWatchWrapperAspectTest {

    static class DemoService {
        @StopWatchWrapper(logHead = "[test]", msg = "demo")
        public String run() {
            return "done";
        }

        @StopWatchWrapper(logHead = "[test]", msg = "slow")
        public String slow() {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "slow-done";
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

    private static DemoService proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new DemoService());
        factory.addAspect(newAspect());
        return factory.getProxy();
    }

    @Test
    void annotatedMethod_stillTimedAndReturnsValue() {
        assertEquals("done", proxy().run(), "有注解的方法必须正常计时并返回业务结果");
    }

    @Test
    void annotationMissing_notInterceptedAndReturnsValue() {
        assertEquals("plain-ok", proxy().plain(), "无注解方法不被 pointcut 匹配，直接执行业务方法，不能 NPE");
    }

    @Test
    void costBetweenWarnAndError_messageMustQuoteWarnThreshold() {
        Logger logger = (Logger) LoggerFactory.getLogger(StopWatchWrapperAspect.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertEquals("slow-done", proxy().slow());
            boolean found = false;
            for (ILoggingEvent event : appender.list) {
                String msg = event.getFormattedMessage();
                if (msg.contains("接口超过")) {
                    found = true;
                    assertTrue(msg.contains("超过200ms"), "仅超 warn 阈值时文案必须标 warn 阈值 200ms，实际: " + msg);
                    assertFalse(msg.contains("超过2000ms"), "未超 error 阈值时文案不得标 error 阈值 2000ms，实际: " + msg);
                }
            }
            assertTrue(found, "超 warn 阈值必须输出告警日志");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}