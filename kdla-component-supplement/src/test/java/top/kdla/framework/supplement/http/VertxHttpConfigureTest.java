package top.kdla.framework.supplement.http;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 H12 回归测试：VertxHttpConfigure 硬编码信任所有证书、无请求超时配置。
 *
 * <p>旧实现两处缺陷：
 * (1) webClientHttps 硬编码 setTrustAll(true)，HTTPS 不校验证书链，存在中间人攻击风险；
 * (2) 未提供单次请求整体超时配置，VertxHttpClient 无超时可用。
 *
 * <p>修复后：trust-all 由 kdla.http.ssl.trust-all 控制且默认 false；
 * kdla.http.request-timeout 默认 30000ms 并注入 VertxHttpClient。
 *
 * <p>无修复则失败：旧代码默认上下文 isTrustAll()==true，且 VertxHttpClient 无 requestTimeoutMs 字段。
 */
class VertxHttpConfigureTest {

    @Test
    void defaultContext_trustAllFalseAndTimeout30s() {
        new ApplicationContextRunner()
                .withUserConfiguration(VertxHttpConfigure.class)
                .run(ctx -> {
                    assertTrue(ctx.containsBean("webClientHttps"));
                    assertFalse(optionsOf(ctx.getBean("webClientHttps", WebClient.class)).isTrustAll(),
                            "默认必须校验证书链");
                    assertEquals(30000L, requestTimeoutOf(ctx.getBean(VertxHttpClient.class)));
                });
    }

    @Test
    void trustAllEnabledAndCustomTimeout_applied() {
        new ApplicationContextRunner()
                .withUserConfiguration(VertxHttpConfigure.class)
                .withPropertyValues("kdla.http.ssl.trust-all=true", "kdla.http.request-timeout=1234")
                .run(ctx -> {
                    assertTrue(optionsOf(ctx.getBean("webClientHttps", WebClient.class)).isTrustAll());
                    assertEquals(1234L, requestTimeoutOf(ctx.getBean(VertxHttpClient.class)));
                });
    }

    @Test
    void disabled_noBeansCreated() {
        new ApplicationContextRunner()
                .withUserConfiguration(VertxHttpConfigure.class)
                .withPropertyValues("kdla.config.vertx-http.enabled=false")
                .run(ctx -> {
                    assertFalse(ctx.containsBean("vertxHttpClient"));
                    assertFalse(ctx.containsBean("webClient"));
                    assertFalse(ctx.containsBean("webClientHttps"));
                    assertFalse(ctx.containsBean("vertx"));
                });
    }

    @Test
    void userDefinedVertxHttpClient_takesPrecedence() {
        VertxHttpClient custom = new VertxHttpClient(null, null);
        new ApplicationContextRunner()
                .withUserConfiguration(VertxHttpConfigure.class)
                .withBean("vertxHttpClient", VertxHttpClient.class, () -> custom)
                .run(ctx -> assertEquals(custom, ctx.getBean("vertxHttpClient")));
    }

    private static WebClientOptions optionsOf(WebClient client) throws Exception {
        Class<?> type = client.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("options");
                field.setAccessible(true);
                return (WebClientOptions) field.get(client);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new AssertionError("未找到WebClient的options字段:" + client.getClass().getName());
    }

    private static long requestTimeoutOf(VertxHttpClient client) throws Exception {
        Field field = VertxHttpClient.class.getDeclaredField("requestTimeoutMs");
        field.setAccessible(true);
        return field.getLong(client);
    }
}
