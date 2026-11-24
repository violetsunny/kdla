package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** D-14 SSRF 回归：内网/链路本地/元数据地址与非 http scheme 必须拒绝。全部用 IP 字面量，不触网。 */
class UrlSecurityUtilTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "http://127.0.0.1/",
            "http://127.0.0.1:8080/admin",
            "http://localhost/",
            "http://0.0.0.0/",
            "http://169.254.169.254/latest/meta-data/",
            "http://10.0.0.1/",
            "http://172.16.0.1/",
            "http://192.168.1.1/",
            "http://[::1]/",
            "http://[fc00::1]/",
            "http://[fd12:3456::1]/",
            "http://[fe80::1]/",
            "http://[::ffff:127.0.0.1]/",
            "http://[::ffff:169.254.169.254]/",
            "ftp://93.184.216.34/",
            "file:///etc/passwd",
            "gopher://93.184.216.34:6379/_INFO",
            "dict://93.184.216.34:6379/",
            "93.184.216.34",
            "//93.184.216.34/",
            "http://",
            "http://93.184.216.34\\@127.0.0.1/",
            "http://internal@127.0.0.1/",
            "http://224.0.0.1/"
    })
    void rejectsUnsafeUrl(String url) {
        assertThat(UrlSecurityUtil.isPublicHttpUrl(url)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://93.184.216.34/",
            "https://93.184.216.34/path?q=1",
            "HTTP://93.184.216.34/",
            "https://93.184.216.34:8443/",
            "http://[2606:2800:220:1:248:1893:25c8:1946]/"
    })
    void acceptsPublicHttpUrl(String url) {
        assertThat(UrlSecurityUtil.isPublicHttpUrl(url)).isTrue();
    }

    @Test
    void rejectsUnresolvableHost() {
        assertThat(UrlSecurityUtil.isPublicHttpUrl("http://host.invalid.kdla.test/")).isFalse();
    }
}
