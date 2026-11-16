package top.kdla.framework.domain;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：ApplicationContextHelp此前存在三处问题：
 * 1) 按type查的空catch静默吞异常；
 * 2) 按name查未命中时NoSuchBeanDefinitionException直接上抛，下方友好报错不可达；
 * 3) context未初始化时调用点直接NPE。
 * 修复后：catch记warn日志、name未命中落入统一友好报错、未初始化抛IllegalStateException。
 */
class ApplicationContextHelpTest {

    static class Foo {
    }

    private ApplicationContext originalContext;
    private Logger helpLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() throws Exception {
        originalContext = (ApplicationContext) getStaticField();
        helpLogger = (Logger) LoggerFactory.getLogger(ApplicationContextHelp.class);
        appender = new ListAppender<>();
        appender.start();
        helpLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() throws Exception {
        helpLogger.detachAppender(appender);
        setStaticField(originalContext);
    }

    private static Object getStaticField() throws Exception {
        Field f = ApplicationContextHelp.class.getDeclaredField("applicationContext");
        f.setAccessible(true);
        return f.get(null);
    }

    private static void setStaticField(ApplicationContext ctx) throws Exception {
        Field f = ApplicationContextHelp.class.getDeclaredField("applicationContext");
        f.setAccessible(true);
        f.set(null, ctx);
    }

    @Test
    void contextNotInitialized_throwsIllegalState_notNPE() throws Exception {
        setStaticField(null);

        // 修复前：applicationContext.getBean(...)直接NPE
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ApplicationContextHelp.getBean(Foo.class));
        assertTrue(e.getMessage().contains("ApplicationContext 未初始化"), "异常消息必须明确指向未初始化");
        assertThrows(IllegalStateException.class, () -> ApplicationContextHelp.getBean("foo"));
        assertThrows(IllegalStateException.class, () -> ApplicationContextHelp.getBeansOfType(Foo.class));
    }

    @Test
    void typeLookupFails_logsWarnAndFallsBackToNameLookup() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        Foo foo = new Foo();
        when(ctx.getBean(Foo.class)).thenThrow(new NoSuchBeanDefinitionException(Foo.class));
        when(ctx.getBean("foo")).thenReturn(foo);
        setStaticField(ctx);

        Foo result = ApplicationContextHelp.getBean(Foo.class);

        assertSame(foo, result, "按type失败后必须按name查到bean");
        // 修复前：空catch静默吞异常，无任何日志线索
        boolean warned = appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("fallback to name lookup"));
        assertTrue(warned, "按type查失败必须记warn日志，不得静默吞异常");
    }

    @Test
    void bothLookupsMiss_friendlyRuntimeExceptionReachable() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBean(Foo.class)).thenThrow(new NoSuchBeanDefinitionException(Foo.class));
        when(ctx.getBean("foo")).thenThrow(new NoSuchBeanDefinitionException("foo"));
        setStaticField(ctx);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> ApplicationContextHelp.getBean(Foo.class));

        // 修复前：按name查的NoSuchBeanDefinitionException直接上抛，友好报错永远不可达
        assertFalse(e instanceof NoSuchBeanDefinitionException, "不得直接上抛NoSuchBeanDefinitionException");
        assertTrue(e.getMessage().contains("can not be found in Spring Container"), "必须落入统一友好报错");
    }
}
