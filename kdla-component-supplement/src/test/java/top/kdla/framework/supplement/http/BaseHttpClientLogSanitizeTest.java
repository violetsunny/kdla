package top.kdla.framework.supplement.http;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 安全审计 D-11 回归测试：BaseHttpClient 在 INFO 级打印完整 URL（含钉钉 access_token）与请求参数/体。
 *
 * <p>修复后：URL 经 maskUrl、参数/体/响应体经 sanitize；异常消息中的 URL 同样打码。
 *
 * <p>无修复则失败：旧代码日志与异常消息含明文 access_token 与 password。
 */
class BaseHttpClientLogSanitizeTest {

    private static final String TOKEN = "ding-token-abc123";
    private static final String URL = "https://oapi.dingtalk.com/robot/send?access_token=" + TOKEN;
    private static final String PASSWORD = "p@ssw0rd";

    private RestTemplate restTemplate;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        logger = (Logger) LoggerFactory.getLogger(BaseHttpClient.class);
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
    void get_masksUrlTokenAndParamCredentials() {
        stubExchange(HttpMethod.GET, HttpStatus.OK, "{\"data\":\"ok\"}");
        Map<String, Object> params = new HashMap<>();
        params.put("password", PASSWORD);

        new BaseHttpClient(restTemplate).get(URL, null, params);

        assertNoSecret();
        assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("access_token=***")),
                "须保留打码后的 URL，实际:" + messages());
    }

    @Test
    void post_masksUrlTokenAndBodyCredentials() {
        stubExchange(HttpMethod.POST, HttpStatus.OK, "{\"data\":\"ok\"}");
        Map<String, Object> body = new HashMap<>();
        body.put("password", PASSWORD);

        new BaseHttpClient(restTemplate).post(URL, null, body);

        assertNoSecret();
    }

    @Test
    void serverError_sanitizesResponseBodyAndExceptionMessage() {
        stubExchange(HttpMethod.GET, HttpStatus.INTERNAL_SERVER_ERROR, "{\"password\":\"" + PASSWORD + "\"}");

        RestClientException e = assertThrows(RestClientException.class,
                () -> new BaseHttpClient(restTemplate).get(URL, null, null));

        assertFalse(e.getMessage().contains(TOKEN), "异常消息不得含明文 token，实际:" + e.getMessage());
        assertNoSecret();
        assertEquals("https://oapi.dingtalk.com/robot/send?access_token=*** 请求异常", e.getMessage());
    }

    private void stubExchange(HttpMethod method, HttpStatus status, String responseBody) {
        when(restTemplate.exchange(any(URI.class), eq(method), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseBody, status));
    }

    private void assertNoSecret() {
        assertFalse(appender.list.isEmpty(), "须有日志输出");
        for (ILoggingEvent event : appender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(TOKEN), "URL 凭据须打码，实际:" + msg);
            assertFalse(msg.contains(PASSWORD), "请求/响应体密码须掩码，实际:" + msg);
        }
    }

    private String messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList().toString();
    }
}
