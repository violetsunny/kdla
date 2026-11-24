package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 安全回归：RSAUtil 旧实现用 SunJCE 默认 PKCS#1 v1.5 填充，存在 Bleichenbacher padding oracle 风险。
 * 修复后显式 OAEP（SHA-256 + MGF1），PKCS#1 v1.5 密文必须解密失败。
 */
class RSAUtilTest {

    @Test
    void encryptDecrypt_roundTrip() throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair();
        String plain = "kdla-rsa-oaep-中文";
        String cipherText = RSAUtil.encrypt(plain, keyPair.getPublic());
        assertEquals(plain, RSAUtil.decrypt(cipherText, keyPair.getPrivate()));
    }

    @Test
    void decrypt_pkcs1v15CipherText_throws() throws Exception {
        // Prove-It：手工用 PKCS#1 v1.5 造密文，OAEP 解密必须失败，证明填充已切换
        KeyPair keyPair = RSAUtil.generateKeyPair();
        Cipher pkcs1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        pkcs1.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        byte[] legacyBytes = pkcs1.doFinal("legacy".getBytes(StandardCharsets.UTF_8));
        String legacyCipherText = Base64.getEncoder().encodeToString(legacyBytes);
        assertThrows(Exception.class, () -> RSAUtil.decrypt(legacyCipherText, keyPair.getPrivate()),
                "PKCS#1 v1.5 密文不得被 OAEP 解密接受");
    }

    @Test
    void keyCodec_base64RoundTrip() throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair();
        String pubBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String priBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        PublicKey publicKey = RSAUtil.getPublicKey(pubBase64);
        PrivateKey privateKey = RSAUtil.getPrivateKey(priBase64);
        String plain = "key-codec";
        assertEquals(plain, RSAUtil.decrypt(RSAUtil.encrypt(plain, publicKey), privateKey));
    }
}