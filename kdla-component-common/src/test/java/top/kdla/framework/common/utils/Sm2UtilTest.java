package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：Sm2Util.decrypt 旧实现 new String(arrayOfByte3) 依赖平台默认字符集，
 * 与 encrypt 侧 getBytes(UTF_8) 不对称，非 UTF-8 默认字符集环境下中文解密乱码。
 * 修复后显式使用 StandardCharsets.UTF_8。
 */
class Sm2UtilTest {

    @Test
    void encryptDecrypt_chineseRoundTrip() throws Exception {
        String[] keyPair = Sm2Util.generateKeyPair("kdla-sm2-regression-seed");
        String plainText = "国密SM2中文加解密回归测试";

        String cipherText = Sm2Util.encrypt(plainText, keyPair[0]);
        String decrypted = Sm2Util.decrypt(cipherText, keyPair[1]);

        assertEquals(plainText, decrypted, "解密必须显式按 UTF-8 还原，中文不能乱码");
    }
}
