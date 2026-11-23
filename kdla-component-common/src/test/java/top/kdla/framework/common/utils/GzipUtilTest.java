package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：compression/unZip 旧实现 getBytes()/toString() 用平台默认字符集，
 * 非 UTF-8 平台中文压缩解压乱码。修复后两侧显式 StandardCharsets.UTF_8。
 */
class GzipUtilTest {

    @Test
    void chineseRoundTrip_restoresOriginal() throws IOException {
        String src = "中文压缩测试 abc123";
        assertEquals(src, GzipUtil.unZip(GzipUtil.compression(src)),
                "中文压缩-解压闭环必须还原原文，不受平台默认字符集影响");
    }

    @Test
    void blankInput_returnsEmpty() throws IOException {
        assertEquals("", GzipUtil.compression(null));
        assertEquals("", GzipUtil.compression("  "));
        assertEquals("", GzipUtil.unZip(null));
        assertEquals("", GzipUtil.unZip(""));
    }
}