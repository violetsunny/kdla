package top.kdla.framework.supplement.netty;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 H11 回归测试：NettyHttpServer 响应体长度错误与异常堆栈外泄。
 *
 * <p>旧实现两处缺陷：
 * (1) writeHttpResponseHtml 用 response.content().writerIndex() 作 CONTENT_LENGTH，
 *     该响应 content 为空，writerIndex 恒 0，客户端按 0 长度截断，HTML 文件内容全部丢失；
 *     且用 DefaultFileRegion(FileChannel, ...) 三参构造，netty 不负责关闭通道，fd 泄漏。
 * (2) dispatcher 抛异常时把 ExceptionUtils.getStackTrace(e) 直接返回客户端，泄露内部实现细节。
 *
 * <p>修复后：CONTENT_LENGTH 取真实文件长度；改用 DefaultFileRegion(File, ...) 由 netty 自管通道；
 * 异常仅记服务端日志，客户端只收到通用文案。
 *
 * <p>无修复则失败：旧代码 CONTENT_LENGTH==0 且 FileRegion 的 f 字段为 null；旧代码响应体含内部异常明细。
 */
class NettyHttpServerResponseTest {

    private static final String INTERNAL_DETAIL = "secret-internal-detail";

    @TempDir
    Path tempDir;

    @Test
    void get_htmlFile_contentLengthIsRealFileLengthAndRegionUsesFileCtor() throws Exception {
        File html = tempDir.resolve("page.html").toFile();
        Files.write(html.toPath(), "<html>hello</html>".getBytes(StandardCharsets.UTF_8));
        long fileLength = html.length();

        EmbeddedChannel channel = new EmbeddedChannel(newHandler((key, t) -> html));
        try {
            channel.writeInbound(request(HttpMethod.GET, "/page", HttpHeaderValues.TEXT_HTML.toString(), null));

            Object first = channel.readOutbound();
            FullHttpResponse response = assertInstanceOf(FullHttpResponse.class, first);
            assertEquals(HttpResponseStatus.OK, response.status());
            assertEquals(fileLength, response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH).longValue());
            assertEquals(HttpHeaderValues.KEEP_ALIVE.toString(),
                    response.headers().get(HttpHeaderNames.CONNECTION));

            Object second = channel.readOutbound();
            FileRegion region = assertInstanceOf(FileRegion.class, second);
            assertInstanceOf(DefaultFileRegion.class, region);
            assertEquals(fileLength, region.count());
            assertEquals(0L, region.position());
            assertSame(html, fileField(region), "须用DefaultFileRegion(File)构造，FileChannel三参构造的f字段为null");

            assertSame(LastHttpContent.EMPTY_LAST_CONTENT, channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void get_dispatcherThrows_returnsGenericMessageAndLogsStackServerSide() throws Exception {
        assertErrorResponse(HttpMethod.GET, null);
    }

    @Test
    void post_dispatcherThrows_returnsGenericMessageAndLogsStackServerSide() throws Exception {
        assertErrorResponse(HttpMethod.POST, "{\"a\":1}");
    }

    private void assertErrorResponse(HttpMethod method, String body) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(handlerClass());
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        EmbeddedChannel channel = new EmbeddedChannel(newHandler((key, t) -> {
            throw new IllegalStateException(INTERNAL_DETAIL);
        }));
        try {
            channel.writeInbound(request(method, "/api", HttpHeaderValues.APPLICATION_JSON.toString(), body));

            FullHttpResponse response = assertInstanceOf(FullHttpResponse.class, channel.readOutbound());
            assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status());
            String responseBody = response.content().toString(CharsetUtil.UTF_8);
            assertTrue(responseBody.contains("服务器内部错误"), "客户端应收到通用文案，实际:" + responseBody);
            assertFalse(responseBody.contains(INTERNAL_DETAIL), "异常明细不得返回客户端，实际:" + responseBody);

            boolean stackLogged = appender.list.stream().anyMatch(e ->
                    e.getLevel() == Level.ERROR && e.getThrowableProxy() != null);
            assertTrue(stackLogged, "完整堆栈须记录在服务端日志");
        } finally {
            channel.finishAndReleaseAll();
            logger.detachAppender(appender);
        }
    }

    private static FullHttpRequest request(HttpMethod method, String uri, String contentType, String body) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri,
                body == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(body, CharsetUtil.UTF_8));
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

    private static Object fileField(FileRegion region) throws Exception {
        Field field = DefaultFileRegion.class.getDeclaredField("f");
        field.setAccessible(true);
        Object value = field.get(region);
        assertNotNull(value, "DefaultFileRegion.f为null说明用了FileChannel构造器");
        return value;
    }
}
