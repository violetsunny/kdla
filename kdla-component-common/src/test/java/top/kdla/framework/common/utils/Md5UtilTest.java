package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：md5 旧实现 getBytes() 用平台默认字符集，非 UTF-8 平台上中文摘要与 UTF-8 基准不一致。
 * 修复后显式 StandardCharsets.UTF_8。基准值按 UTF-8 编码计算。
 */
class Md5UtilTest {

    @Test
    void md5_ascii_knownVector() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", Md5Util.md5("hello"));
    }

    @Test
    void md5_chinese_matchesUtf8Vector() {
        assertEquals("a7bac2239fcdcb3a067903d8077c4a07", Md5Util.md5("中文"),
                "中文摘要必须按 UTF-8 编码计算，不受平台默认字符集影响");
    }
}