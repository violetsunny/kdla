package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：encrypt/decrypt 密钥长度校验失败时，旧实现把 password 明文拼进异常消息
 * （"Invalid key size. " + password），异常一旦上抛/入日志即泄露密钥。
 * 修复后仅保留长度信息。
 */
class AESUtilTest {

    private static final String SHORT_PASSWORD = "shortPwd";

    @Test
    void encrypt_wrongKeySize_messageMustNotLeakPassword() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> AESUtil.encrypt("data", SHORT_PASSWORD));
        assertFalse(ex.getMessage().contains(SHORT_PASSWORD), "异常消息禁止携带密钥明文");
        assertTrue(ex.getMessage().contains(String.valueOf(SHORT_PASSWORD.length())),
                "异常消息应保留密钥长度信息便于排障");
    }

    @Test
    void decrypt_wrongKeySize_messageMustNotLeakPassword() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> AESUtil.decrypt("v1:ZGF0YQ==", SHORT_PASSWORD));
        assertFalse(ex.getMessage().contains(SHORT_PASSWORD), "异常消息禁止携带密钥明文");
        assertTrue(ex.getMessage().contains(String.valueOf(SHORT_PASSWORD.length())),
                "异常消息应保留密钥长度信息便于排障");
    }

    @Test
    void encryptDecrypt_roundTrip_stillWorks() {
        String password = "1234567890abcdef";
        String cipherText = AESUtil.encrypt("hello 中国", password);
        assertEquals("hello 中国", AESUtil.decrypt(cipherText, password), "正常加解密功能不受影响");
    }
}
