package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：SM4 由 ECB 迁移到 CBC+随机 IV（v1: 前缀，对齐 AESUtil 约定）。
 * 旧实现 ECB 无 IV，相同明文块产生相同密文块；且 generateKey 用低熵种子播种
 * SecureRandom，同 keySrc 必派生同密钥。修复后：
 * 1. 新加密输出 v1: 前缀且同明文两次加密密文不同（随机 IV 生效）；
 * 2. 无前缀的旧 ECB 密文仍可解密（兼容分支）；
 * 3. generateSecureKey 真随机，两次生成密钥不同且可完成加解密闭环。
 */
class Sm4UtilCbcMigrationTest {

    @Test
    void encrypt_outputsV1Prefix_andRandomIvMakesCipherDifferentEachTime() throws Exception {
        String key = Sm4Util.generateSecureKey();
        String plaintext = "你好，SM4！Hello, 123";

        String c1 = Sm4Util.encrypt(plaintext, key);
        String c2 = Sm4Util.encrypt(plaintext, key);

        assertTrue(c1.startsWith("v1:"), "新密文必须带 v1: 前缀");
        assertNotEquals(c1, c2, "随机 IV 下同一明文两次加密密文必须不同（ECB 缺陷已消除）");
        assertEquals(plaintext, Sm4Util.decrypt(c1, key));
        assertEquals(plaintext, Sm4Util.decrypt(c2, key));
    }

    @Test
    void decrypt_legacyEcbCipherWithoutPrefix_stillRestoresPlaintext() throws Exception {
        String key = Sm4Util.generateSecureKey();
        String plaintext = "legacy-ecb-data";

        // 用旧 ECB 方式手工构造存量密文（无 v1: 前缀）
        byte[] keyBytes = Base64.getDecoder().decode(key);
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "SM4"));
        String legacyCipher = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes("utf-8")));

        assertEquals(plaintext, Sm4Util.decrypt(legacyCipher, key),
                "无前缀的旧 ECB 密文必须仍可解密（兼容分支）");
    }

    @Test
    void generateSecureKey_isRandom_andCompletesEncryptDecryptRoundTrip() throws Exception {
        String k1 = Sm4Util.generateSecureKey();
        String k2 = Sm4Util.generateSecureKey();
        assertNotEquals(k1, k2, "真随机密钥两次生成必须不同");

        String plaintext = "round-trip-验证";
        assertEquals(plaintext, Sm4Util.decrypt(Sm4Util.encrypt(plaintext, k1), k1));
    }
}
