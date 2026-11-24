package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：AES 由 CBC 升级到 GCM（v2: 前缀）。
 * CBC 无完整性校验，密文被篡改可能解出垃圾明文或仅触发填充错误；
 * GCM 为 AEAD，篡改密文/tag 必抛异常。v1 存量密文必须仍可解。
 */
class AESUtilGcmTest {

    private static final String PASSWORD = "1234567890abcdef";
    private static final int NONCE_LENGTH = 12;

    @Test
    void encrypt_outputsV2Prefix() {
        assertTrue(AESUtil.encrypt("data", PASSWORD).startsWith("v2:"),
                "新密文必须带 v2: 前缀（GCM 方案），旧实现输出 v1: 即失败");
    }

    @Test
    void decrypt_v2RoundTrip_utf8() {
        String plaintext = "hello 中国 — AES-GCM 验证";
        assertEquals(plaintext, AESUtil.decrypt(AESUtil.encrypt(plaintext, PASSWORD), PASSWORD));
    }

    @Test
    void decrypt_v2TamperedCiphertext_mustThrow() {
        byte[] combined = Base64.getDecoder()
                .decode(AESUtil.encrypt("integrity-protected", PASSWORD).substring(3));
        combined[NONCE_LENGTH] ^= 0x01;
        String tampered = "v2:" + Base64.getEncoder().encodeToString(combined);
        assertThrows(RuntimeException.class, () -> AESUtil.decrypt(tampered, PASSWORD),
                "GCM tag 校验必须拒绝被篡改的密文");
    }

    @Test
    void decrypt_v2TamperedTag_mustThrow() {
        byte[] combined = Base64.getDecoder()
                .decode(AESUtil.encrypt("integrity-protected", PASSWORD).substring(3));
        combined[combined.length - 1] ^= 0x01;
        String tampered = "v2:" + Base64.getEncoder().encodeToString(combined);
        assertThrows(RuntimeException.class, () -> AESUtil.decrypt(tampered, PASSWORD),
                "GCM tag 被篡改必须拒绝");
    }

    @Test
    void decrypt_v1CbcCipher_stillWorks() throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(PASSWORD.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(iv));
        byte[] enc = cipher.doFinal("legacy-v1".getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + enc.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(enc, 0, combined, iv.length, enc.length);
        String v1 = "v1:" + Base64.getEncoder().encodeToString(combined);
        assertEquals("legacy-v1", AESUtil.decrypt(v1, PASSWORD), "v1 存量密文必须仍可解");
    }

    /**
     * NIST GCM Test Case 2：K=0^128, IV=0^96, P=0^128,
     * C=0388dace60b6a392f328c2b971b2fe78, T=ab6e47d42cec13bdf53a67b21257bddf。
     * 已知答案向量证明 v2 是标准 AES-GCM，而非自洽的私有格式。
     */
    @Test
    void decrypt_nistGcmKnownAnswerVector() {
        byte[] cipherAndTag = hex("0388dace60b6a392f328c2b971b2fe78ab6e47d42cec13bdf53a67b21257bddf");
        byte[] combined = new byte[NONCE_LENGTH + cipherAndTag.length];
        System.arraycopy(cipherAndTag, 0, combined, NONCE_LENGTH, cipherAndTag.length);
        String v2 = "v2:" + Base64.getEncoder().encodeToString(combined);
        String zeroKey = "\0".repeat(16);
        assertEquals("\0".repeat(16), AESUtil.decrypt(v2, zeroKey), "必须匹配 NIST AES-GCM 已知答案向量");
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
