package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：generateKey 产出的密钥文本是 Base64 编码，
 * 而 encrypt/decrypt(String,String) 曾用 Hex.decode 解析密钥，
 * Base64 尾部的 '=' 填充字符会让 Hex.decode 直接抛 DecoderException，加解密全链路不可用。
 * 修复后统一走 Base64（strKeyToSecretKey），生成的密钥必须能直接完成加密-解密闭环。
 */
class Sm4UtilKeyCodecTest {

    @Test
    void generatedKey_encryptThenDecrypt_restoresPlaintext() throws Exception {
        String secretKeyText = Sm4Util.generateKey("kdla-sm4-regression-seed");
        String plaintext = "你好，SM4！Hello, 123";

        String cipherText = Sm4Util.encrypt(plaintext, secretKeyText);
        String decrypted = Sm4Util.decrypt(cipherText, secretKeyText);

        assertEquals(plaintext, decrypted,
                "generateKey() 产出的 Base64 密钥必须能直接用于 encrypt/decrypt 闭环还原明文");
    }
}
