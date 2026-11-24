package top.kdla.framework.supplement.netty;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全审计 D-15/D-11 回归测试。
 *
 * <p>D-15：NettyHttpServer 五个响应分支均缺 nosniff/no-store/X-Frame-Options。
 * <p>D-11：服务端 INFO 日志序列化整个 FullHttpRequest（含 Authorization/Cookie）且 uri 未打码。
 *
 * <p>无修复则失败：旧代码响应头三项皆 null；旧代码 INFO 日志含 Bearer 凭据与明文 access_token。
 */
class NettyHttpServerSecurityHeaderTest {

    private static final String TOKEN = "ding-token-abc123";
    private static final String BEARER = "Bearer eyJhbGciOiJIUzI1NiJ9.secret";

    @TempDir
    Path tempDir;

    @Test
    void jsonResponse_hasSecurityHeaders() throws Exception {
        assertSecurityHeaders(HttpHeaderValues.APPLICATION_JSON.toString(), "ok");
    }

    @Test
    void htmlStringResponse_hasSecurityHeaders() throws Exception {
        assertSecurityHeaders(HttpHeaderValues.TEXT_HTML.toString(), "<html>ok</html>");
    }

    @Test
    void plainResponse_hasSecurityHeaders() throws Exception {
        assertSecurityHeaders(HttpHeaderValues.TEXT_PLAIN.toString(), "ok");
    }

    @Test
    void htmlFileResponse_hasSecurityHeaders() throws Exception {
        File html = tempDir.resolve("page.html").toFile();
        Files.write(html.toPath(), "<html>hello</html>".getBytes(StandardCharsets.UTF_8));
        assertSecurityHeaders(HttpHeaderValues.TEXT_HTML.toString(), html);
    }

    @Test
    void imageResponse_hasSecurityHeaders() throws Exception {
        File png = tempDir.resolve("a.png").toFile();
        Files.write(png.toPath(), new byte[]{1, 2, 3});
        assertSecurityHeaders("image/png", png);
    }

    @Test
    void requestLog_masksUrlTokenAndOmitsCredentialHeaders() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(handlerClass());
        Level original = logger.getLevel();
        logger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        EmbeddedChannel channel = new EmbeddedChannel(newHandler((key, t) -> "ok"));
        try {
            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                    "/robot/send?access_token=" + TOKEN, Unpooled.EMPTY_BUFFER);
            request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            request.headers().set(HttpHeaderNames.AUTHORIZATION, BEARER);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            channel.writeInbound(request);
            channel.readOutbound();

            assertFalse(appender.list.isEmpty(), "须有 INFO 访问日志");
            for (ILoggingEvent event : appender.list) {
                String msg = event.getFormattedMessage();
                assertFalse(msg.contains(TOKEN), "URL 凭据须打码，实际:" + msg);
                assertFalse(msg.contains(BEARER), "Authorization 头不得进日志，实际:" + msg);
            }
            assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("access_token=***")),
                    "须保留打码后的 URL 便于定位");
        } finally {
            channel.finishAndReleaseAll();
            logger.detachAppender(appender);
            logger.setLevel(original);
        }
    }

    private void assertSecurityHeaders(String contentType, Object result) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(newHandler((key, t) -> result));
        try {
            channel.writeInbound(request(contentType));
            FullHttpResponse response = assertInstanceOf(FullHttpResponse.class, channel.readOutbound());
            assertEquals("nosniff", response.headers().get("X-Content-Type-Options"));
            assertEquals("no-store", response.headers().get(HttpHeaderNames.CACHE_CONTROL));
            assertEquals("DENY", response.headers().get(HttpHeaderNames.X_FRAME_OPTIONS));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private static FullHttpRequest request(String contentType) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api",
                Unpooled.EMPTY_BUFFER);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        return request;
    }

    private static ChannelHandler newHandler(NettyHttpServer.DispatcherServer<?> dispatcher) throws Exception {
        Constructor<?> constructor = handlerClass().getDeclaredConstructor(NettyHttpServer.DispatcherServer.class);
        constructor.setAccessible(true);
        return (ChannelHandler) constructor.newInstance(dispatcher);
    }

    private static Class<?> handlerClass() throws ClassNotFoundException {
        return Class.forName("top.kdla.framework.supplement.netty.NettyHttpServer$HttpServerInboundHandlerJson");
    }
}
