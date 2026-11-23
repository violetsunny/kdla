package top.kdla.framework.supplement.http;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 M11 回归测试：HttpContextHelp.ignoreSSL 静默篡改 JVM 全局 SSL 默认配置。
 *
 * <p>旧实现缺陷：ignoreSSL 无任何告警地替换 HttpsURLConnection 的默认 SSLSocketFactory
 * 与 HostnameVerifier，宿主进程内所有 HTTPS 连接（含第三方库）都不再校验证书，
 * 调用方完全不知情；方法也未标记废弃，无迁移指引。
 *
 * <p>修复后：方法标 @Deprecated 并给出连接级替代方案；调用时输出两条 WARN 明示全局影响。
 *
 * <p>无修复则失败：旧代码无 @Deprecated 注解，且调用后无任何 WARN 日志。
 */
class HttpContextHelpIgnoreSslTest {

    @Test
    void ignoreSSL_warnsAboutGlobalMutation() throws Exception {
        SSLSocketFactory originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier originalVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        Logger logger = (Logger) LoggerFactory.getLogger(HttpContextHelp.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            HttpContextHelp.ignoreSSL();

            assertTrue(hasWarn(appender, "trustAllHttpsCertificates invoked"), "须告警信任所有证书");
            assertTrue(hasWarn(appender, "ignoreSSL已修改JVM全局"), "须告警JVM全局影响");
            assertNotSame(originalFactory, HttpsURLConnection.getDefaultSSLSocketFactory(),
                    "全局SSLSocketFactory确已被替换");
            assertNotSame(originalVerifier, HttpsURLConnection.getDefaultHostnameVerifier(),
                    "全局HostnameVerifier确已被替换");
        } finally {
            logger.detachAppender(appender);
            HttpsURLConnection.setDefaultSSLSocketFactory(originalFactory);
            HttpsURLConnection.setDefaultHostnameVerifier(originalVerifier);
        }
    }

    @Test
    void ignoreSSL_isDeprecated() throws Exception {
        Method method = HttpContextHelp.class.getMethod("ignoreSSL");
        assertTrue(method.isAnnotationPresent(Deprecated.class), "ignoreSSL须标记@Deprecated");
    }

    private static boolean hasWarn(ListAppender<ILoggingEvent> appender, String fragment) {
        return appender.list.stream().anyMatch(e ->
                e.getLevel() == Level.WARN && e.getFormattedMessage().contains(fragment));
    }
}
