package top.kdla.framework.supplement.http;

import cn.hutool.http.HttpConfig;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import cn.hutool.http.ssl.DefaultSSLInfo;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 安全审计 D-01 回归测试：KdlaHttpClient 继承 hutool HttpUtil，默认 trust-all + 恒真域名校验。
 *
 * <p>修复后显式恢复 JDK 默认信任链与默认 HostnameVerifier。
 *
 * <p>无修复则失败：旧代码 config.ssf/hostnameVerifier 为 null，
 * 运行时 HttpConnection 回退 DefaultSSLInfo trust-any。
 */
class KdlaHttpClientSslTest {

    @Test
    void buildRequest_usesJdkDefaultTrustAndHostnameVerification() throws Exception {
        HttpRequest request = KdlaHttpClient.buildRequest(Method.GET, "https://example.com");
        HttpConfig config = (HttpConfig) field(HttpRequest.class, "config").get(request);
        Object ssf = field(HttpConfig.class, "ssf").get(config);
        Object verifier = field(HttpConfig.class, "hostnameVerifier").get(config);
        assertNotNull(ssf, "须显式设置SSLSocketFactory，null会回退hutool trust-any");
        assertNotSame(DefaultSSLInfo.DEFAULT_SSF, ssf, "不得使用hutool trust-all工厂");
        assertSame(HttpsURLConnection.getDefaultHostnameVerifier(), verifier, "须用JDK默认域名校验");
    }

    private static Field field(Class<?> clazz, String name) throws NoSuchFieldException {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
