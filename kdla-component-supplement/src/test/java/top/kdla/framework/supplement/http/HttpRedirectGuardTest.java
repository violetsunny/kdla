package top.kdla.framework.supplement.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * D-14 回归测试：HTTP 客户端跟随重定向可绕过首跳 URL 校验（SSRF）。
 *
 * <p>旧实现：Vertx WebClient 未设 maxRedirects（默认 16 跳），HttpClient5 未禁重定向（默认跟随），
 * 公网首跳返回 302 指向内网地址时客户端自动跟进，UrlSecurityUtil 的首跳校验被绕过。
 *
 * <p>修复后：两个 WebClient 均 setMaxRedirects(0)，HttpClient5 disableRedirectHandling()，
 * 3xx 原样返回给调用方，重定向决策权交回调用方（可先校验 Location 再自行发起）。
 *
 * <p>无修复则失败：旧代码 Vertx 拿到 200 "followed"，BaseHttpClient 返回 200 而非抛 RestClientException。
 */
class HttpRedirectGuardTest {

    @Test
    void vertxClientDoesNotFollowRedirect() throws Exception {
        HttpServer server = startServer();
        try {
            new ApplicationContextRunner()
                    .withUserConfiguration(VertxHttpConfigure.class)
                    .run(ctx -> {
                        VertxHttpClient client = ctx.getBean(VertxHttpClient.class);
                        HttpResponse<Buffer> response = assertTimeoutPreemptively(Duration.ofSeconds(10),
                                () -> client.sendRequest("GET", url(server, "/start"), null, null)
                                        .get(8, TimeUnit.SECONDS));
                        assertEquals(302, response.statusCode(), "3xx须原样返回，不得自动跟进");
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void restClientDoesNotFollowRedirect() throws Exception {
        HttpServer server = startServer();
        try {
            new ApplicationContextRunner()
                    .withUserConfiguration(RestHttpConfigure.class)
                    .withPropertyValues("kdla.config.rest-http.enabled=true")
                    .run(ctx -> {
                        BaseHttpClient client = ctx.getBean(BaseHttpClient.class);
                        assertThrows(RestClientException.class,
                                () -> client.get(url(server, "/start"), null, null),
                                "3xx须原样返回并由BaseHttpClient判为异常，不得自动跟进");
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void bothWebClientsHaveZeroMaxRedirects() {
        new ApplicationContextRunner()
                .withUserConfiguration(VertxHttpConfigure.class)
                .run(ctx -> {
                    assertEquals(0, optionsOf(ctx.getBean("webClient", WebClient.class)).getMaxRedirects());
                    assertEquals(0, optionsOf(ctx.getBean("webClientHttps", WebClient.class)).getMaxRedirects());
                });
    }

    @Test
    void normalRequestStillWorks() throws Exception {
        HttpServer server = startServer();
        try {
            new ApplicationContextRunner()
                    .withUserConfiguration(VertxHttpConfigure.class)
                    .run(ctx -> {
                        VertxHttpClient client = ctx.getBean(VertxHttpClient.class);
                        HttpResponse<Buffer> response = assertTimeoutPreemptively(Duration.ofSeconds(10),
                                () -> client.sendRequest("GET", url(server, "/ok"), null, null)
                                        .get(8, TimeUnit.SECONDS));
                        assertEquals(200, response.statusCode());
                        assertEquals("ok", response.bodyAsString());
                    });
        } finally {
            server.stop(0);
        }
    }

    private static String url(HttpServer server, String path) {
        return "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort() + path;
    }

    private static HttpServer startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().set("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> respond(exchange, "followed"));
        server.createContext("/ok", exchange -> respond(exchange, "ok"));
        server.start();
        return server;
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
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
}
