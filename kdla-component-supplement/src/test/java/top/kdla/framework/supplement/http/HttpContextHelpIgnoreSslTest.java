package top.kdla.framework.supplement.http;

import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全审计 D-03/D-06 回归测试：ignoreSSL 污染 JVM 全局 SSL 默认、buildCors 反射任意 Origin 带凭据。
 *
 * <p>修复后两个危险兼容 API 一律抛 UnsupportedOperationException fail-closed，
 * 不再修改任何全局状态；迁移路径见各自 javadoc。
 *
 * <p>无修复则失败：旧代码正常返回并替换全局 SSLSocketFactory/HostnameVerifier。
 */
class HttpContextHelpIgnoreSslTest {

    @Test
    void ignoreSSL_throwsAndKeepsGlobalDefaults() {
        SSLSocketFactory originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier originalVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        assertThrows(UnsupportedOperationException.class, HttpContextHelp::ignoreSSL);
        assertSame(originalFactory, HttpsURLConnection.getDefaultSSLSocketFactory(), "全局SSLSocketFactory不得被修改");
        assertSame(originalVerifier, HttpsURLConnection.getDefaultHostnameVerifier(), "全局HostnameVerifier不得被修改");
    }

    @Test
    void buildCors_throwsFailClosed() {
        assertThrows(UnsupportedOperationException.class, () -> HttpContextHelp.buildCors(null, null));
    }

    @Test
    void dangerousApis_remainDeprecated() throws Exception {
        Method ignoreSsl = HttpContextHelp.class.getMethod("ignoreSSL");
        assertTrue(ignoreSsl.isAnnotationPresent(Deprecated.class), "ignoreSSL须保留@Deprecated");
        Method buildCors = HttpContextHelp.class.getMethod("buildCors",
                jakarta.servlet.http.HttpServletRequest.class, jakarta.servlet.http.HttpServletResponse.class);
        assertTrue(buildCors.isAnnotationPresent(Deprecated.class), "buildCors须保留@Deprecated");
    }
}
