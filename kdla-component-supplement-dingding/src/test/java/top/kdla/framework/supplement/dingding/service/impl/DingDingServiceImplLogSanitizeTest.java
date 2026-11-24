package top.kdla.framework.supplement.dingding.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vertx.core.http.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import top.kdla.framework.supplement.dingding.config.DingAlertConfigure;
import top.kdla.framework.supplement.dingding.enums.MsgSendRespEnum;
import top.kdla.framework.supplement.dingding.model.res.DingDingAlertResult;
import top.kdla.framework.supplement.dingding.properties.DingAlertProperties;
import top.kdla.framework.supplement.http.VertxHttpClient;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 安全审计 D-13 回归测试：钉钉发送失败日志明文打印 atMobiles 手机号与消息体敏感字段。
 *
 * <p>修复后：atMobiles 经 maskMobile（前3后4），title/msg/result 经 sanitize。
 *
 * <p>无修复则失败：旧代码日志含完整手机号与明文 password。
 */
class DingDingServiceImplLogSanitizeTest {

    private static final String MOBILE = "13812348000";
    private static final String MSG = "{\"password\":\"SuperSecret123\",\"detail\":\"disk full\"}";

    private DingDingServiceImpl service;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() throws Exception {
        DingAlertProperties properties = mock(DingAlertProperties.class);
        when(properties.getDingKeywords()).thenReturn("kw");
        when(properties.getAppName()).thenReturn("app");
        DingAlertConfigure configure = mock(DingAlertConfigure.class);
        when(configure.isDingAlertEnable()).thenReturn(true);
        when(configure.getDingUrl()).thenReturn("https://oapi.dingtalk.com/robot/send?access_token=token");
        when(configure.getEnv()).thenReturn("test");
        when(configure.getDingAlertProperties()).thenReturn(properties);

        VertxHttpClient httpClient = mock(VertxHttpClient.class);
        DingDingAlertResult fail = DingDingAlertResult.builder().errcode("310000").errmsg("keywords not in content").build();
        when(httpClient.sendRequest(any(HttpMethod.class), anyString(), any(), any(), eq(DingDingAlertResult.class)))
                .thenReturn(CompletableFuture.completedFuture(fail));

        service = new DingDingServiceImpl();
        inject("dingAlertConfigure", configure);
        inject("vertxHttpClient", httpClient);

        logger = (Logger) LoggerFactory.getLogger(DingDingServiceImpl.class);
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
    void sendMessage_withAtMobiles_masksMobilesAndSensitiveMsg() {
        String code = service.sendMessage("disk alert", MSG, List.of(MOBILE, "13987654321"));

        assertEquals(MsgSendRespEnum.FAIL.getCode(), code);
        assertMasked();
        assertTrue(messages().contains("138****8000"), "手机号须保留前3后4，实际:" + messages());
        assertTrue(messages().contains("139****4321"), "手机号须保留前3后4，实际:" + messages());
    }

    @Test
    void sendTextMessage_withAtMobiles_masksMobiles() {
        String code = service.sendTextMessage(MSG, List.of(MOBILE));

        assertEquals(MsgSendRespEnum.FAIL.getCode(), code);
        assertMasked();
        assertTrue(messages().contains("138****8000"), "手机号须保留前3后4，实际:" + messages());
    }

    @Test
    void sendMessage_withoutAtMobiles_sanitizesTitleAndMsg() {
        String code = service.sendMessage("{\"apiKey\":\"titleSecret\"}", MSG);

        assertEquals(MsgSendRespEnum.FAIL.getCode(), code);
        assertMasked();
        assertFalse(messages().contains("titleSecret"), "title 敏感字段须掩码，实际:" + messages());
    }

    @Test
    void sendTextMessage_nullAtMobiles_noNpe() {
        String code = service.sendTextMessage(MSG, null);

        assertEquals(MsgSendRespEnum.FAIL.getCode(), code);
        assertMasked();
        assertTrue(messages().contains("atMobiles:null"), "null 列表须安全输出，实际:" + messages());
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = DingDingServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private void assertMasked() {
        assertFalse(appender.list.isEmpty(), "须有失败日志");
        for (ILoggingEvent event : appender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(MOBILE), "手机号不得明文进日志，实际:" + msg);
            assertFalse(msg.contains("13987654321"), "手机号不得明文进日志，实际:" + msg);
            assertFalse(msg.contains("SuperSecret123"), "消息体密码须掩码，实际:" + msg);
        }
    }

    private String messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList().toString();
    }
}