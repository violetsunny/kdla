package top.kdla.framework.supplement.http;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import top.kdla.framework.exception.BizException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 安全审计 D-11 回归测试：VertxHttpClient 解析响应失败时 WARN 打印完整 URL（含钉钉 access_token）与响应体。
 *
 * <p>修复后：URL 经 maskUrl、请求/响应体经 sanitize。
 *
 * <p>无修复则失败：旧代码 WARN 含明文 access_token 与 password。
 */
class VertxHttpClientLogSanitizeTest {

    private static final String TOKEN = "ding-token-abc123";
    private static final String URL = "https://oapi.dingtalk.com/robot/send?access_token=" + TOKEN;
    private static final String BODY = "{\"password\":\"p@ssw0rd\",\"access_token\":\"" + TOKEN + "\"}";

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(VertxHttpClient.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
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
    @SuppressWarnings("unchecked")
    void parseFailure_warnMasksUrlTokenAndBodyCredentials() {
        WebClient webClient = mock(WebClient.class);
        HttpRequest<Buffer> request = mock(HttpRequest.class);
        HttpResponse<Buffer> response = mock(HttpResponse.class);
        when(webClient.postAbs(URL)).thenReturn(request);
        when(request.putHeaders(any())).thenReturn(request);
        when(request.timeout(anyLong())).thenReturn(request);
        when(request.sendJson(any())).thenReturn(Future.succeededFuture(response));
        when(response.bodyAsJson(Object.class)).thenThrow(new RuntimeException("parse error"));
        when(response.bodyAsString()).thenReturn(BODY);

        VertxHttpClient client = new VertxHttpClient(mock(WebClient.class), webClient);
        CompletableFuture<Object> future = client.sendRequest(HttpMethod.POST, URL, null,
                Map.of("password", "reqSecret"), Object.class);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(BizException.class, ex.getCause());

        assertFalse(appender.list.isEmpty(), "须有 WARN 日志");
        boolean maskedUrl = false;
        for (ILoggingEvent event : appender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(TOKEN), "URL/响应体凭据须打码，实际:" + msg);
            assertFalse(msg.contains("p@ssw0rd"), "响应体密码须掩码，实际:" + msg);
            assertFalse(msg.contains("reqSecret"), "请求体密码须掩码，实际:" + msg);
            maskedUrl |= msg.contains("access_token=***");
        }
        assertTrue(maskedUrl, "须保留打码后的 URL，实际:" + appender.list);
    }
}