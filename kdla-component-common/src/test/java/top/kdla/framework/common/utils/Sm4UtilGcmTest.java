package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：SM4 由 CBC 升级到 GCM（v2: 前缀）。
 * GCM 为 AEAD，篡改密文/tag 必抛异常；v1 存量密文必须仍可解；
 * v2 须能被原生 BC SM4/GCM/NoPadding 解开，证明是标准格式而非私有封装。
 */
class Sm4UtilGcmTest {

    private static final int NONCE_LENGTH = 12;

    @Test
    void encrypt_outputsV2Prefix() throws Exception {
        String key = Sm4Util.generateSecureKey();
        assertTrue(Sm4Util.encrypt("data", key).startsWith("v2:"),
                "新密文必须带 v2: 前缀（GCM 方案），旧实现输出 v1: 即失败");
    }

    @Test
    void decrypt_v2RoundTrip_utf8() throws Exception {
        String key = Sm4Util.generateSecureKey();
        String plaintext = "你好，SM4-GCM！Hello, 123";
        assertEquals(plaintext, Sm4Util.decrypt(Sm4Util.encrypt(plaintext, key), key));
    }

    @Test
    void byteArrayKeyOverload_v2RoundTrip() throws Exception {
        byte[] key = Base64.getDecoder().decode(Sm4Util.generateSecureKey());
        String plaintext = "byte[] 密钥重载";
        String cipherText = Sm4Util.encrypt(plaintext, key);
        assertTrue(cipherText.startsWith("v2:"), "byte[] 密钥重载同样输出 v2");
        assertEquals(plaintext, Sm4Util.decrypt(cipherText, key));
    }

    @Test
    void decrypt_v2TamperedCiphertext_mustThrow() throws Exception {
        String key = Sm4Util.generateSecureKey();
        byte[] combined = Base64.getDecoder().decode(Sm4Util.encrypt("integrity", key).substring(3));
        combined[NONCE_LENGTH] ^= 0x01;
        String tampered = "v2:" + Base64.getEncoder().encodeToString(combined);
        assertThrows(Exception.class, () -> Sm4Util.decrypt(tampered, key),
                "GCM tag 校验必须拒绝被篡改的密文");
    }

    @Test
    void decrypt_v2TamperedTag_mustThrow() throws Exception {
        String key = Sm4Util.generateSecureKey();
        byte[] combined = Base64.getDecoder().decode(Sm4Util.encrypt("integrity", key).substring(3));
        combined[combined.length - 1] ^= 0x01;
        String tampered = "v2:" + Base64.getEncoder().encodeToString(combined);
        assertThrows(Exception.class, () -> Sm4Util.decrypt(tampered, key), "GCM tag 被篡改必须拒绝");
    }

    @Test
    void decrypt_v1CbcCipher_stillWorks() throws Exception {
        String key = Sm4Util.generateSecureKey();
        byte[] keyBytes = Base64.getDecoder().decode(key);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("SM4/CBC/PKCS5Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "SM4"), new IvParameterSpec(iv));
        byte[] enc = cipher.doFinal("legacy-v1".getBytes("utf-8"));
        byte[] combined = new byte[iv.length + enc.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(enc, 0, combined, iv.length, enc.length);
        String v1 = "v1:" + Base64.getEncoder().encodeToString(combined);
        assertEquals("legacy-v1", Sm4Util.decrypt(v1, key), "v1 存量密文必须仍可解");
    }

    @Test
    void v2Cipher_isStandardSm4Gcm_interopWithRawBcCipher() throws Exception {
        String key = Sm4Util.generateSecureKey();
        byte[] keyBytes = Base64.getDecoder().decode(key);
        String plaintext = "interop-验证";
        byte[] combined = Base64.getDecoder().decode(Sm4Util.encrypt(plaintext, key).substring(3));
        byte[] nonce = new byte[NONCE_LENGTH];
        byte[] body = new byte[combined.length - NONCE_LENGTH];
        System.arraycopy(combined, 0, nonce, 0, NONCE_LENGTH);
        System.arraycopy(combined, NONCE_LENGTH, body, 0, body.length);
        Cipher cipher = Cipher.getInstance("SM4/GCM/NoPadding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "SM4"), new GCMParameterSpec(128, nonce));
        assertEquals(plaintext, new String(cipher.doFinal(body), "utf-8"),
                "v2 必须是标准 SM4-GCM，可被原生 BC Cipher 直接解开");
    }
}
