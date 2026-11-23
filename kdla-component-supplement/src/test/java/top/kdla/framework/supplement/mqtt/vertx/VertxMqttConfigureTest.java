package top.kdla.framework.supplement.mqtt.vertx;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import top.kdla.framework.domain.ApplicationContextHelp;
import top.kdla.framework.supplement.mqtt.MqttHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 清单 C4 回归测试：VertxMqttConfigure 无条件装配、topics 无缺省值、订阅空白 topic。
 *
 * <p>旧实现三处缺陷：
 * (1) 类上无 @ConditionalOnProperty，未启用 mqtt 的应用也被强制创建 MqttClient 并连接 broker；
 * (2) topics 的 @Value 无缺省值，未配置 kdla.mqtt.topics 时启动直接失败；
 * (3) topics 缺省解析为空串列表后不过滤，向 broker 订阅空 topic。
 *
 * <p>修复后：enabled=true 才装配；topics 缺省空；subscribe 过滤空白项。
 *
 * <p>无修复则失败：旧代码默认上下文即创建 mqttClient Bean、@Value 无 ":" 缺省、
 * subscribe("") 被调用。
 */
class VertxMqttConfigureTest {

    @AfterEach
    void clearStaticContext() throws Exception {
        Field f = ApplicationContextHelp.class.getDeclaredField("applicationContext");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void defaultContext_mqttBeansNotCreated() {
        new ApplicationContextRunner()
                .withUserConfiguration(VertxMqttConfigure.class)
                .run(ctx -> {
                    assertFalse(ctx.containsBean("mqttClient"));
                    assertFalse(ctx.containsBean("vertxMqttClient"));
                });
    }

    @Test
    void enabledWithExistingMqttClient_wrapsBean() {
        new ApplicationContextRunner()
                .withUserConfiguration(VertxMqttConfigure.class)
                .withPropertyValues("kdla.config.mqtt.enabled=true")
                .withBean(Vertx.class, () -> mock(Vertx.class))
                .withBean(MqttClient.class, () -> mock(MqttClient.class))
                .run(ctx -> {
                    assertTrue(ctx.containsBean("vertxMqttClient"));
                    assertInstanceOf(VertxMqttClient.class, ctx.getBean("vertxMqttClient"));
                });
    }

    @Test
    void topicsValue_hasEmptyDefault() throws Exception {
        Field f = VertxMqttConfigure.class.getDeclaredField("topics");
        Value value = f.getAnnotation(Value.class);
        assertEquals("${kdla.mqtt.topics:}", value.value());
    }

    @Test
    void subscribe_blankTopicsFiltered() throws Exception {
        VertxMqttConfigure configure = new VertxMqttConfigure();
        setField(configure, "topics", List.of("", "t1", "   "));
        setField(configure, "qos", 0);
        ApplicationContext appCtx = mock(ApplicationContext.class);
        when(appCtx.getBeansOfType(MqttHandler.class)).thenReturn(Collections.emptyMap());
        new ApplicationContextHelp().setApplicationContext(appCtx);

        MqttClient mqttClient = mock(MqttClient.class);
        when(mqttClient.subscribe(anyString(), anyInt())).thenReturn(Future.succeededFuture(1));
        Method m = VertxMqttConfigure.class.getDeclaredMethod("subscribe", MqttClient.class);
        m.setAccessible(true);
        m.invoke(configure, mqttClient);

        verify(mqttClient).subscribe("t1", 0);
        verify(mqttClient, never()).subscribe(eq(""), anyInt());
        verify(mqttClient, never()).subscribe(eq("   "), anyInt());
        verify(mqttClient).publishHandler(any());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}