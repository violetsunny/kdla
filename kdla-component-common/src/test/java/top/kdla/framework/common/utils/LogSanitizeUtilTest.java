package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：请求体/Header/响应此前无脱敏无截断直接打印。
 * 修复后：黑名单Header（authorization/cookie/password/token/mobile/blade-auth，忽略大小写）值替换为***；
 * 日志体超2KB（2048字符）截断并追加"...(truncated, total=原始长度)"。
 */
class LogSanitizeUtilTest {

    @Test
    void maskHeader_blacklistNames_maskedIgnoreCase() {
        assertEquals("***", LogSanitizeUtil.maskHeader("authorization", "Bearer abc"));
        assertEquals("***", LogSanitizeUtil.maskHeader("Authorization", "Bearer abc"), "忽略大小写");
        assertEquals("***", LogSanitizeUtil.maskHeader("COOKIE", "SESSION=xyz"));
        assertEquals("***", LogSanitizeUtil.maskHeader("password", "p@ss"));
        assertEquals("***", LogSanitizeUtil.maskHeader("Token", "t-123"));
        assertEquals("***", LogSanitizeUtil.maskHeader("mobile", "13800000000"));
        // E-02：BladeX JWT 头 blade-auth 此前漏脱敏，RequestLogAspect 全量打印请求头时明文进日志可重放
        assertEquals("***", LogSanitizeUtil.maskHeader("blade-auth", "eyJhbGciOiJIUzI1NiJ9.xxx"));
        assertEquals("***", LogSanitizeUtil.maskHeader("Blade-Auth", "eyJhbGciOiJIUzI1NiJ9.xxx"), "忽略大小写");
    }

    @Test
    void maskHeader_nonBlacklist_unchanged() {
        assertEquals("application/json", LogSanitizeUtil.maskHeader("Content-Type", "application/json"));
        assertEquals("v1", LogSanitizeUtil.maskHeader(null, "v1"), "名称为null时原样返回");
    }

    @Test
    void truncate_shortBody_unchanged() {
        assertNull(LogSanitizeUtil.truncate(null));
        assertEquals("abc", LogSanitizeUtil.truncate("abc"));
        // 恰好2048字符不截断
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2048; i++) {
            sb.append('a');
        }
        assertEquals(2048, LogSanitizeUtil.truncate(sb.toString()).length());
    }

    @Test
    void truncate_overSizeBody_truncatedWithTotalMarker() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            sb.append('x');
        }
        String result = LogSanitizeUtil.truncate(sb.toString());
        assertTrue(result.startsWith("xxxx"), "保留前2048字符");
        assertTrue(result.endsWith("...(truncated, total=3000)"), "追加原始长度标记，实际=" + result.substring(result.length() - 40));
        assertEquals(2048 + "...(truncated, total=3000)".length(), result.length());
    }

    @Test
    void maskHeader_expandedBlacklistAndVariants_masked() {
        // D-09：扩充黑名单与变体匹配，旧精确匹配下这些头全部明文落日志
        assertEquals("***", LogSanitizeUtil.maskHeader("Set-Cookie", "JSESSIONID=abc"));
        assertEquals("***", LogSanitizeUtil.maskHeader("X-Api-Key", "ak-123"));
        assertEquals("***", LogSanitizeUtil.maskHeader("apikey", "ak-123"));
        assertEquals("***", LogSanitizeUtil.maskHeader("X-Access-Token", "t-123"));
        assertEquals("***", LogSanitizeUtil.maskHeader("refresh-token", "r-123"));
        assertEquals("***", LogSanitizeUtil.maskHeader("X-Auth-Token", "t-456"));
        assertEquals("***", LogSanitizeUtil.maskHeader("sessionid", "s-1"));
        assertEquals("***", LogSanitizeUtil.maskHeader("JSESSIONID", "s-2"));
        assertEquals("***", LogSanitizeUtil.maskHeader("X-Csrf-Token", "c-1"));
        assertEquals("***", LogSanitizeUtil.maskHeader("Proxy-Authorization", "Basic xyz"));
        assertEquals("***", LogSanitizeUtil.maskHeader("secret", "s3cr3t"));
        assertEquals("***", LogSanitizeUtil.maskHeader("credential", "cred"));
    }

    @Test
    void maskJson_sensitiveFields_maskedRecursively() {
        // D-10：请求体/响应体字段级掩码，旧实现只截断不脱敏
        String json = "{\"user\":\"u1\",\"password\":\"p@ss\",\"nested\":{\"token\":\"t-1\"},"
                + "\"arr\":[{\"mobile\":\"13800000000\",\"idCard\":\"110101199911111234\"}]}";
        String masked = LogSanitizeUtil.maskJson(json);
        assertFalse(masked.contains("p@ss"), "password 值必须掩码");
        assertFalse(masked.contains("t-1"), "嵌套 token 值必须掩码");
        assertFalse(masked.contains("13800000000"), "数组内 mobile 必须掩码");
        assertFalse(masked.contains("110101199911111234"), "idCard 必须掩码");
        assertTrue(masked.contains("u1"), "非敏感字段保留");
        assertTrue(masked.contains("***"));
    }

    @Test
    void maskJson_nonJsonOrScalar_unchanged() {
        assertNull(LogSanitizeUtil.maskJson(null));
        assertEquals("plain text body", LogSanitizeUtil.maskJson("plain text body"));
        assertEquals("123", LogSanitizeUtil.maskJson("123"));
    }

    @Test
    void sanitize_masksAndTruncates() {
        StringBuilder big = new StringBuilder("{\"password\":\"p\",\"data\":\"");
        for (int i = 0; i < 3000; i++) {
            big.append('z');
        }
        big.append("\"}");
        String result = LogSanitizeUtil.sanitize(big.toString());
        assertFalse(result.contains("\"p\""), "敏感值须掩码");
        // total 标记基于掩码后的 JSON 长度（掩码会改变串长）
        int maskedLength = LogSanitizeUtil.maskJson(big.toString()).length();
        assertTrue(result.endsWith("...(truncated, total=" + maskedLength + ")"), "超长须截断");
    }

    @Test
    void validTraceId_whitelist() {
        // D-12：外来 traceId 格式白名单，CRLF/超长/非法字符一律拒绝
        assertTrue(LogSanitizeUtil.validTraceId("upstream-trace-1"));
        assertTrue(LogSanitizeUtil.validTraceId("ABCdef0123456789-"));
        assertFalse(LogSanitizeUtil.validTraceId(null));
        assertFalse(LogSanitizeUtil.validTraceId(""));
        assertFalse(LogSanitizeUtil.validTraceId("abc\r\n2026-11-24 INFO fake"));
        assertFalse(LogSanitizeUtil.validTraceId("abc\nxyz"));
        assertFalse(LogSanitizeUtil.validTraceId("trace_id"));
        StringBuilder over = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            over.append('a');
        }
        assertFalse(LogSanitizeUtil.validTraceId(over.toString()), "超过64字符拒绝");
    }

    @Test
    void maskUrl_dingdingAccessToken() {
        // D-11：钉钉 webhook token 在 URL query 里，不得随日志明文落盘
        assertEquals("https://oapi.dingtalk.com/robot/send?access_token=***",
                LogSanitizeUtil.maskUrl("https://oapi.dingtalk.com/robot/send?access_token=abc123"));
    }

    @Test
    void maskUrl_keepsNonSensitiveParamsAndOrder() {
        assertEquals("https://api.example.com/v1?name=ok&password=***&page=2",
                LogSanitizeUtil.maskUrl("https://api.example.com/v1?name=ok&password=p@ss&page=2"));
    }

    @Test
    void maskUrl_caseInsensitiveAndRepeatedParams() {
        assertEquals("https://a.com?X-Access-Token=***&token=***",
                LogSanitizeUtil.maskUrl("https://a.com?X-Access-Token=t1&token=t2"));
    }

    @Test
    void maskUrl_noQueryOrNull_unchanged() {
        assertNull(LogSanitizeUtil.maskUrl(null));
        assertEquals("https://a.com/path", LogSanitizeUtil.maskUrl("https://a.com/path"));
        assertEquals("https://a.com/path?", LogSanitizeUtil.maskUrl("https://a.com/path?"));
    }

    @Test
    void maskUrl_preservesFragmentAndValuelessParam() {
        assertEquals("https://a.com?flag&token=***#sec",
                LogSanitizeUtil.maskUrl("https://a.com?flag&token=t#sec"));
    }
}
