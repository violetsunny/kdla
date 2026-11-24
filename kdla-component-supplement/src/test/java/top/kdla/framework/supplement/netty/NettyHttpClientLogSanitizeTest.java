package top.kdla.framework.supplement.netty;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全审计 D-11 回归测试：NettyHttpClient 在 INFO 级打印全部响应头（含 Set-Cookie）与完整响应体。
 *
 * <p>修复后：响应头/体降为 DEBUG，且头值经 maskHeader、体经 sanitize 掩码。
 *
 * <p>无修复则失败：旧代码 INFO 级即输出 JSESSIONID 与明文 password。
 */
class NettyHttpClientLogSanitizeTest {

    private static final String COOKIE = "JSESSIONID=abc123";
    private static final String BODY = "{\"password\":\"p@ssw0rd\",\"data\":\"ok\"}";

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() throws Exception {
        logger = (Logger) LoggerFactory.getLogger(handlerClass());
        originalLevel = logger.getLevel();
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    @Test
    void infoLevel_doesNotDumpHeadersOrBody() throws Exception {
        logger.setLevel(Level.INFO);
        CompletableFuture<String> response = fireResponse();
        assertEquals(BODY, response.get());
        assertTrue(appender.list.isEmpty(), "INFO 级不得输出响应头/体，实际:" + appender.list);
    }

    @Test
    void debugLevel_masksCookieAndBodyCredentials() throws Exception {
        logger.setLevel(Level.DEBUG);
        fireResponse();
        boolean cookieMasked = appender.list.stream()
                .anyMatch(e -> e.getFormattedMessage().equalsIgnoreCase("HEADER: Set-Cookie = ***"));
        assertTrue(cookieMasked, "Set-Cookie 须打码，实际:" + messages());
        for (ILoggingEvent event : appender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(COOKIE), "会话凭据不得明文进日志，实际:" + msg);
            assertFalse(msg.contains("p@ssw0rd"), "响应体密码须掩码，实际:" + msg);
        }
        assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().startsWith("content:")),
                "DEBUG 级仍须保留响应体便于定位");
    }

    private CompletableFuture<String> fireResponse() throws Exception {
        Constructor<?> constructor = handlerClass().getDeclaredConstructor();
        constructor.setAccessible(true);
        ChannelHandler handler = (ChannelHandler) constructor.newInstance();
        CompletableFuture<String> future = new CompletableFuture<>();
        Method setter = handlerClass().getDeclaredMethod("setResponse", CompletableFuture.class);
        setter.setAccessible(true);
        setter.invoke(handler, future);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        try {
            FullHttpResponse msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(BODY, CharsetUtil.UTF_8));
            msg.headers().set(HttpHeaderNames.SET_COOKIE, COOKIE);
            msg.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            channel.writeInbound(msg);
        } finally {
            channel.finishAndReleaseAll();
        }
        return future;
    }

    private String messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList().toString();
    }

    private static Class<?> handlerClass() throws ClassNotFoundException {
        return Class.forName("top.kdla.framework.supplement.netty.NettyHttpClient$OutputResultHandlerCompletable");
    }
}
