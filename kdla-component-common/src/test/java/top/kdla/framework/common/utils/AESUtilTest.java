package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Test
    void encrypt_sameContentTwice_ciphertextMustDiffer() {
        String password = "1234567890abcdef";
        String c1 = AESUtil.encrypt("same-plaintext", password);
        String c2 = AESUtil.encrypt("same-plaintext", password);
        assertTrue(c1.startsWith("v1:"), "新密文必须带 v1 前缀（随机 IV 方案）");
        assertNotEquals(c1, c2, "随机 IV 必须使同明文两次加密的密文不同，全零 IV 会产出相同密文");
    }

    @Test
    void decrypt_legacyZeroIvFormat_stillWorks() throws Exception {
        String password = "1234567890abcdef";
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(new byte[16]));
        String legacy = Base64.getEncoder().encodeToString(cipher.doFinal("hello".getBytes(StandardCharsets.UTF_8)));
        assertEquals("hello", AESUtil.decrypt(legacy, password), "无前缀的旧全零 IV 存量密文必须仍可解");
    }
}
