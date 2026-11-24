package top.kdla.framework.common.utils;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：A/C-13 Sm2Util 密钥双轨编码 + C1C2C3/C1C3C2 双模式支持。
 * 修复后 encrypt/decrypt/sign/verify 均自动识别 Base64(X509/PKCS8) 与 hex 裸密钥；
 * encrypt 默认 C1C3C2、可指定 C1C2C3，decrypt 自动识别两种模式。
 * 无修复则失败：旧 sign/verify 仅收 hex、旧 encrypt/decrypt 仅收 Base64、旧 decrypt 仅 C1C2C3。
 */
class Sm2UtilDualEncodingTest {

    private static final String PLAIN = "国密SM2双编码双模式回归";

    @Test
    void signVerify_base64Keys() throws Exception {
        String[] pair = Sm2Util.generateSecureKeyPair();
        String sig = Sm2Util.sign(PLAIN, pair[1]);
        assertTrue(Sm2Util.verify(PLAIN, sig, pair[0]), "sign/verify 须支持 Base64(X509/PKCS8) 密钥");
    }

    @Test
    void signVerify_hexKeys() throws Exception {
        String[] pair = Sm2Util.generateSecureKeyPair();
        String sig = Sm2Util.sign(PLAIN, hexPrivate(pair[1]));
        assertTrue(Sm2Util.verify(PLAIN, sig, hexPublic(pair[0])), "sign/verify 须兼容 hex 裸密钥");
    }

    @Test
    void signVerify_crossEncoding() throws Exception {
        String[] pair = Sm2Util.generateSecureKeyPair();
        String sigFromHex = Sm2Util.sign(PLAIN, hexPrivate(pair[1]));
        assertTrue(Sm2Util.verify(PLAIN, sigFromHex, pair[0]), "hex 私钥签名须能用 Base64 公钥验签");
        String sigFromB64 = Sm2Util.sign(PLAIN, pair[1]);
        assertTrue(Sm2Util.verify(PLAIN, sigFromB64, hexPublic(pair[0])), "Base64 私钥签名须能用 hex 公钥验签");
    }

    @Test
    void encryptDecrypt_hexKeys() throws Exception {
        String[] pair = Sm2Util.generateSecureKeyPair();
        String cipher = Sm2Util.encrypt(PLAIN, hexPublic(pair[0]));
        assertEquals(PLAIN, Sm2Util.decrypt(cipher, hexPrivate(pair[1])), "encrypt/decrypt 须支持 hex 裸密钥");
    }

    @Test
    void encryptDecrypt_crossEncoding() throws Exception {
        String[] pair = Sm2Util.generateSecureKeyPair();
        String cipherFromHex = Sm2Util.encrypt(PLAIN, hexPublic(pair[0]));
        assertEquals(PLAIN, Sm2Util.decrypt(cipherFromHex, pair[1]), "hex 公钥加密须能用 Base64 私钥解密");
        String cipherFromB64 = Sm2Util.encrypt(PLAIN, pair[0]);
        assertEquals(PLAIN, Sm2Util.decrypt(cipherFromB64, hexPrivate(pair[1])), "Base64 公钥加密须能用 hex 私钥解密");
    }

    @Test
    void decrypt_autoDetectsC1C2C3() throws Exception {
        String[] pair = Sm2Util.generateSecureKeyPair();
        String cipher = Sm2Util.encrypt(PLAIN, pair[0], SM2Engine.Mode.C1C2C3);
        assertEquals(PLAIN, Sm2Util.decrypt(cipher, pair[1]), "decrypt 须自动识别 C1C2C3 密文");
    }

    @Test
    void decrypt_autoDetectsC1C3C2Default() throws Exception {
        String[] pair = Sm2Util.generateSecureKeyPair();
        String cipher = Sm2Util.encrypt(PLAIN, pair[0]);
        assertEquals(PLAIN, Sm2Util.decrypt(cipher, pair[1]), "默认 C1C3C2 密文须可解（旧 decrypt 仅 C1C2C3 会失败）");
    }

    @Test
    void hexPrivate_highBitSet_parsedUnsigned() throws Exception {
        // 确定性回归：D 最高位为 1，new BigInteger(byte[]) 会解成负数致 "Scalar not in [1,n-1]"，须 signum=1
        BigInteger d = new BigInteger("ff" + "00".repeat(31), 16);
        X9ECParameters params = GMNamedCurves.getByOID(GMObjectIdentifiers.sm2p256v1);
        ECPoint q = params.getG().multiply(d).normalize();
        String hexD = String.format("%064x", d);
        String hexQ = Hex.toHexString(q.getEncoded(false));

        String cipher = Sm2Util.encrypt(PLAIN, hexQ);
        assertEquals(PLAIN, Sm2Util.decrypt(cipher, hexD), "高位为 1 的 hex 私钥须按无符号标量解析");
        String sig = Sm2Util.sign(PLAIN, hexD);
        assertTrue(Sm2Util.verify(PLAIN, sig, hexQ), "高位为 1 的 hex 私钥签名须可验");
    }

    private static String hexPrivate(String base64Pkcs8) throws Exception {
        byte[] der = Base64.getDecoder().decode(base64Pkcs8);
        PrivateKey pk = KeyFactory.getInstance("EC", new BouncyCastleProvider())
                .generatePrivate(new PKCS8EncodedKeySpec(der));
        return String.format("%064x", ((BCECPrivateKey) pk).getD());
    }

    private static String hexPublic(String base64X509) throws Exception {
        byte[] der = Base64.getDecoder().decode(base64X509);
        PublicKey pk = KeyFactory.getInstance("EC", new BouncyCastleProvider())
                .generatePublic(new X509EncodedKeySpec(der));
        return Hex.toHexString(((BCECPublicKey) pk).getQ().getEncoded(false));
    }
}