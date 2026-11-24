package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全回归：Sm3Util.verify 旧实现用 Arrays.equals 比较摘要，存在时序侧信道；
 * 修复后 MessageDigest.isEqual 恒时比较，行为不变（正确 true / 篡改 false）。
 */
class Sm3UtilTest {

    @Test
    void verify_correctHash_true() {
        String hash = Sm3Util.encrypt("kdla-sm3", "key123");
        assertTrue(Sm3Util.verify("kdla-sm3", "key123", hash));
    }

    @Test
    void verify_tamperedData_false() {
        String hash = Sm3Util.encrypt("kdla-sm3", "key123");
        assertFalse(Sm3Util.verify("kdla-sm3-tampered", "key123", hash));
    }

    @Test
    void verify_wrongKey_false() {
        String hash = Sm3Util.encrypt("kdla-sm3", "key123");
        assertFalse(Sm3Util.verify("kdla-sm3", "key456", hash));
    }
}