package top.kdla.framework.supplement.http;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 M5 回归测试：RestHttpConfigure 连接池无 TTL，复用被服务端静默断开的连接。
 *
 * <p>旧实现缺陷：PoolingHttpClientConnectionManager 只设 maxTotal/maxPerRoute，
 * 未设连接存活时间，池中连接可无限期复用，服务端已关闭时请求偶发 NoHttpResponseException。
 *
 * <p>修复后：setDefaultConnectionConfig 设 timeToLive=60s（HttpClient5 的 TTL 由 ConnectionConfig 承载）。
 *
 * <p>无修复则失败：旧代码 connectionConfigResolver 为 null，反射取值直接 NPE/断言失败。
 */
class RestHttpConfigureTtlTest {

    @Test
    void defaultContext_restHttpNotAssembled() {
        new ApplicationContextRunner()
                .withUserConfiguration(RestHttpConfigure.class)
                .run(ctx -> {
                    assertFalse(ctx.containsBean("httpClient"));
                    assertFalse(ctx.containsBean("restTemplate"));
                });
    }

    @Test
    void enabled_connectionTimeToLiveIs60Seconds() {
        new ApplicationContextRunner()
                .withUserConfiguration(RestHttpConfigure.class)
                .withPropertyValues("kdla.config.rest-http.enabled=true")
                .run(ctx -> {
                    assertTrue(ctx.containsBean("httpClient"));
                    HttpClient httpClient = ctx.getBean("httpClient", HttpClient.class);
                    assertEquals(TimeValue.of(60, TimeUnit.SECONDS), timeToLiveOf(httpClient));
                });
    }

    private static TimeValue timeToLiveOf(HttpClient httpClient) throws Exception {
        Field connManagerField = httpClient.getClass().getDeclaredField("connManager");
        connManagerField.setAccessible(true);
        Object manager = connManagerField.get(httpClient);
        assertInstanceOf(PoolingHttpClientConnectionManager.class, manager);

        Field resolverField = PoolingHttpClientConnectionManager.class.getDeclaredField("connectionConfigResolver");
        resolverField.setAccessible(true);
        Object resolver = resolverField.get(manager);
        assertTrue(resolver instanceof Resolver, "未设置defaultConnectionConfig，连接池无TTL");

        @SuppressWarnings("unchecked")
        Resolver<Object, ConnectionConfig> typed = (Resolver<Object, ConnectionConfig>) resolver;
        return typed.resolve(null).getTimeToLive();
    }
}
