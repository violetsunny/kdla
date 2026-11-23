package top.kdla.framework.log.sanitize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：请求体/Header/响应此前无脱敏无截断直接打印。
 * 修复后：黑名单Header（authorization/cookie/password/token/mobile，忽略大小写）值替换为***；
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
}
