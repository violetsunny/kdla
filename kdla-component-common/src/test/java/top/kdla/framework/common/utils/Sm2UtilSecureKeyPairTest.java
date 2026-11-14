package top.kdla.framework.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 回归测试：generateKeyPair(keySrc) 用低熵种子播种 SecureRandom，
 * 同 keySrc 必派生同私钥（已标 @Deprecated 保留兼容）。
 * 新增 generateSecureKeyPair() 必须真随机且能完成加解密闭环。
 */
class Sm2UtilSecureKeyPairTest {

    @Test
    void generateSecureKeyPair_isRandom_andCompletesEncryptDecryptRoundTrip() throws Exception {
        String[] pair1 = Sm2Util.generateSecureKeyPair();
        String[] pair2 = Sm2Util.generateSecureKeyPair();
        assertNotEquals(pair1[1], pair2[1], "真随机密钥对两次生成私钥必须不同");

        String plaintext = "hello 国密 SM2";
        String cipher = Sm2Util.encrypt(plaintext, pair1[0]);
        assertEquals(plaintext, Sm2Util.decrypt(cipher, pair1[1]));
    }

    @Test
    void deprecatedGenerateKeyPair_stillProducesUsableKeyPair() throws Exception {
        // 兼容承诺：旧 API 保留且可用。注意实测当前 BC/JDK8 环境下同 keySrc 两次生成
        // 私钥并不相同（文档宣称的确定性实际不成立），故只断言功能闭环不断言确定性
        String[] pair = Sm2Util.generateKeyPair("kdla-sm2-seed");
        String plaintext = "compat 验证";
        assertEquals(plaintext, Sm2Util.decrypt(Sm2Util.encrypt(plaintext, pair[0]), pair[1]));
    }
}
