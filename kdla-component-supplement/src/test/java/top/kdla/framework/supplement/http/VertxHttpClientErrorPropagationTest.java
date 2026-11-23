package top.kdla.framework.supplement.http;

import com.sun.net.httpserver.HttpServer;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * 清单 H13 回归测试：VertxHttpClient 异常无法到达调用方、无请求超时。
 *
 * <p>旧实现两处缺陷：
 * (1) sendRequest 回调内 completeExceptionally(e.getCause())，解析异常 cause 为 null 时
 *     CompletableFuture 抛 NPE；随后 throw 的 BizException 运行在 Vert.x 事件循环线程，
 *     调用方 future 永不完成，get() 永久阻塞。
 * (2) createRequest 从不设置 request.timeout，服务端接受连接但不响应时 future 永不完成。
 *
 * <p>修复后：cause 为 null 回退用 e 本身，异常统一经 future 传递；requestTimeoutMs>0 时设置请求超时。
 *
 * <p>无修复则失败：旧代码两个场景下 future.get 均抛 java.util.concurrent.TimeoutException（future 未完成），
 * 而非 ExecutionException。
 */
class VertxHttpClientErrorPropagationTest {

    private Vertx vertx;
    private WebClient webClientHttp;
    private WebClient webClientHttps;
    private VertxHttpClient client;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        webClientHttp = WebClient.create(vertx);
        webClientHttps = WebClient.create(vertx);
        client = new VertxHttpClient(webClientHttp, webClientHttps);
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    void invalidJsonBody_failureDeliveredAsBizException() throws Exception {
        HttpServer server = startServer(exchange -> respond(exchange, "{invalid-json"));
        try {
            String url = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort() + "/bad";
            CompletableFuture<Payload> future = client.sendRequest("GET", url, null, null, Payload.class);

            assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
                ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(8, TimeUnit.SECONDS));
                assertInstanceOf(BizException.class, ex.getCause(), "解析异常须以BizException经future传递");
            });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void serverNeverResponds_requestTimeoutCompletesFuture() throws Exception {
        try (ServerSocket silent = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            String url = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + silent.getLocalPort() + "/silent";
            client.setRequestTimeoutMs(500);
            CompletableFuture<Payload> future = client.sendRequest("GET", url, null, null, Payload.class);

            assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
                ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(8, TimeUnit.SECONDS));
                assertInstanceOf(TimeoutException.class, ex.getCause(), "请求超时须以TimeoutException完成future");
            });
        }
    }

    @Test
    void validJsonBody_stillCompletesNormally() throws Exception {
        HttpServer server = startServer(exchange -> respond(exchange, "{\"name\":\"ok\"}"));
        try {
            String url = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + server.getAddress().getPort() + "/good";
            CompletableFuture<Payload> future = client.sendRequest("GET", url, null, null, Payload.class);

            Payload payload = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> future.get(8, TimeUnit.SECONDS));
            assertEquals("ok", payload.name);
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", handler);
        server.start();
        return server;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static class Payload {
        public String name;
    }
}
