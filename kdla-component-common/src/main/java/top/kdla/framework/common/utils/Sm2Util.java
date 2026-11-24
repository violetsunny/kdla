/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * 国密算法 - SM2加密工具：用来替换RSA，属于非对称加密，需要公钥和私钥
 * <pre>
 *   它是基于椭圆曲线密码的公钥密码算法标准，其秘钥长度256bit，包含数字签名、密钥交换和公钥加密，用于替换RSA/DH/ECDSA/ECDH等国际算法。
 *   可以满足电子认证服务系统等应用需求，由国家密码管理局于2010年12月17号发布。
 *   SM2采用的是ECC 256位的一种，其安全强度比RSA 2048位高，且运算速度快于RSA。
 * </pre>
 * <p>密钥编码兼容：encrypt/decrypt/sign/verify 均自动识别 Base64(X509/PKCS8) 与 hex 裸公钥点/裸私钥 D，可混用。
 * <p>密文模式：encrypt 默认 C1C3C2（现行 GB/T 32918），重载可指定 C1C2C3；decrypt 自动识别两种模式。
 *
 * @author wangMaoXiong
 * @version 1.0
 * @date 2024/4/13 14:58
 **/
public class Sm2Util {
    private static final String CHARSET_UTF8 = "utf-8";
    private static final String STD_NAME = "sm2p256v1";
    private static final String ALGORITHM = "EC";
    private static final Logger log = LoggerFactory.getLogger(Sm2Util.class);

    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

    static {
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(PROVIDER);
        }
    }

    /**
     * 生成国密公私钥对（真随机）。
     * <p>生产环境优先使用本方法；私钥应经密钥管理设施存储与分发。
     *
     * @return ：返回公公私密钥(base64)，[0] 公钥、[1] 私钥
     * @throws Exception
     */
    public static String[] generateSecureKeyPair() throws Exception {
        ECGenParameterSpec sm2Spec = new ECGenParameterSpec(STD_NAME);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(sm2Spec);
        return formatKeyPair(keyPairGenerator.generateKeyPair());
    }

    /**
     * 生成国密公私钥对
     *
     * @param keySrc ：生成公私密钥的随机种子，可以任意随机。
     * @return ：返回公公私密钥(base64)，[0] 公钥、[1] 私钥
     * @throws Exception
     * @deprecated 低熵种子播种SecureRandom，同keySrc必派生同私钥，keySrc可猜测即密钥体系全破。
     *             仅为兼容既有数据保留，新场景使用 {@link #generateSecureKeyPair()}
     */
    @Deprecated
    public static String[] generateKeyPair(String keySrc) throws Exception {
        SecureRandom secureRandom = new SecureRandom(keySrc.getBytes(CHARSET_UTF8));
        ECGenParameterSpec sm2Spec = new ECGenParameterSpec(STD_NAME);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM, new BouncyCastleProvider());
        keyPairGenerator.initialize(sm2Spec);
        keyPairGenerator.initialize(sm2Spec, secureRandom);
        return formatKeyPair(keyPairGenerator.generateKeyPair());
    }

    private static String[] formatKeyPair(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        String publicKeyText = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
        String privateKeyText = new String(Base64.getEncoder().encode(privateKey.getEncoded()));
        // String[0] 公钥
        // String[1] 私钥
        return new String[]{publicKeyText, privateKeyText};
    }

    /**
     * 公钥加密（默认 C1C3C2，现行 GB/T 32918）
     *
     * @param data          ：被加密的数据
     * @param publicKeyText ：公钥文本，Base64(X509) 或 hex 裸公钥点，自动识别
     * @return ：加密好的密文(base64)
     */
    public static String encrypt(String data, String publicKeyText) throws Exception {
        return encrypt(data, publicKeyText, SM2Engine.Mode.C1C3C2);
    }

    /**
     * 公钥加密（指定密文模式）
     *
     * @param data          ：被加密的数据
     * @param publicKeyText ：公钥文本，Base64(X509) 或 hex 裸公钥点，自动识别
     * @param mode          ：C1C3C2（现行标准）或 C1C2C3（旧序，兼容对端）
     * @return ：加密好的密文(base64)
     */
    public static String encrypt(String data, String publicKeyText, SM2Engine.Mode mode) throws Exception {
        BCECPublicKey publicKey = (BCECPublicKey) createPublicKey(publicKeyText);
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ECParameterSpec spec = publicKey.getParameters();
        ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
        ECPublicKeyParameters pubParams = new ECPublicKeyParameters(publicKey.getQ(), domain);
        SM2Engine engine = new SM2Engine(mode);
        engine.init(true, new ParametersWithRandom(pubParams, new SecureRandom()));
        byte[] out = engine.processBlock(bytes, 0, bytes.length);
        return Base64.getEncoder().encodeToString(out);
    }

    /**
     * 私钥解密（自动识别 C1C3C2/C1C2C3 两种密文模式）
     *
     * @param data           ：密文(base64)
     * @param privateKeyText ：私钥文本，Base64(PKCS8) 或 hex 裸私钥 D，自动识别
     * @return ：明文
     */
    public static String decrypt(String data, String privateKeyText) throws Exception {
        BCECPrivateKey sm2PriK = (BCECPrivateKey) createPrivateKey(privateKeyText);
        byte[] cipher = Base64.getDecoder().decode(data);
        ECParameterSpec spec = sm2PriK.getParameters();
        ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
        ECPrivateKeyParameters privParams = new ECPrivateKeyParameters(sm2PriK.getD(), domain);
        // C3 为 SM3 校验位，模式不符必抛 InvalidCipherTextException，据此自动识别两种模式
        InvalidCipherTextException last = null;
        for (SM2Engine.Mode mode : new SM2Engine.Mode[]{SM2Engine.Mode.C1C3C2, SM2Engine.Mode.C1C2C3}) {
            try {
                SM2Engine engine = new SM2Engine(mode);
                engine.init(false, privParams);
                byte[] plain = engine.processBlock(cipher, 0, cipher.length);
                // 显式 UTF-8，与 encrypt 侧 getBytes(UTF_8) 对齐，避免平台默认字符集导致中文乱码
                return new String(plain, StandardCharsets.UTF_8);
            } catch (InvalidCipherTextException e) {
                last = e;
            }
        }
        throw last;
    }

    /**
     * 公钥串转公钥对象：自动识别 hex 裸公钥点（04/02/03 前缀）与 Base64(X509)
     *
     * @param publicKeyText hex 裸公钥点或 Base64(X509)
     * @return 公钥对象
     */
    private static PublicKey createPublicKey(String publicKeyText) throws Exception {
        if (isHexRaw(publicKeyText, 130, 66)) {
            X9ECParameters parameters = GMNamedCurves.getByOID(GMObjectIdentifiers.sm2p256v1);
            ECPoint q = parameters.getCurve().decodePoint(Hex.decode(publicKeyText));
            ECParameterSpec spec = new ECParameterSpec(parameters.getCurve(), parameters.getG(), parameters.getN());
            return new BCECPublicKey("EC", new ECPublicKeySpec(q, spec), BouncyCastleProvider.CONFIGURATION);
        }
        byte[] decode = Base64.getDecoder().decode(publicKeyText);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decode);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * 私钥串转私钥对象：自动识别 hex 裸私钥 D（64 hex，32 字节零填充）与 Base64(PKCS8)
     *
     * @param privateKeyText hex 裸私钥 D 或 Base64(PKCS8)
     * @return 私钥对象
     */
    private static PrivateKey createPrivateKey(String privateKeyText) throws Exception {
        if (isHexRaw(privateKeyText, 64)) {
            X9ECParameters parameters = GMNamedCurves.getByOID(GMObjectIdentifiers.sm2p256v1);
            ECParameterSpec spec = new ECParameterSpec(parameters.getCurve(), parameters.getG(), parameters.getN());
            // signum=1：裸 D 是无符号标量，防最高位为 1 时 new BigInteger(byte[]) 解成负数
            BigInteger d = new BigInteger(1, Hex.decode(privateKeyText));
            return new BCECPrivateKey("EC", new ECPrivateKeySpec(d, spec), BouncyCastleProvider.CONFIGURATION);
        }
        byte[] decode = Base64.getDecoder().decode(privateKeyText);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(decode);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }

    /**
     * 判定是否指定长度的纯 hex 串（裸密钥编码识别用）
     */
    private static boolean isHexRaw(String text, int... hexLengths) {
        if (text == null) {
            return false;
        }
        boolean lenMatch = false;
        for (int len : hexLengths) {
            if (text.length() == len) {
                lenMatch = true;
                break;
            }
        }
        if (!lenMatch) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.digit(text.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 签名
     *
     * @param plainText  待签名文本
     * @param privateKey 私钥文本，hex 裸私钥 D 或 Base64(PKCS8)，自动识别
     * @return 签名(base64)
     * @throws GeneralSecurityException
     */
    public static String sign(String plainText, String privateKey) throws Exception {
        BCECPrivateKey pk = (BCECPrivateKey) createPrivateKey(privateKey);
        return sign(plainText, pk.getD());
    }

    public static String sign(String plainText, BigInteger privateKey) throws GeneralSecurityException {
        X9ECParameters parameters = GMNamedCurves.getByOID(GMObjectIdentifiers.sm2p256v1);
        ECParameterSpec parameterSpec = new ECParameterSpec(parameters.getCurve(), parameters.getG(), parameters.getN());
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKey, parameterSpec);
        PrivateKey bcecPrivateKey = new BCECPrivateKey("EC", privateKeySpec, BouncyCastleProvider.CONFIGURATION);
        // 创建签名对象
        Signature signature = Signature.getInstance(GMObjectIdentifiers.sm2sign_with_sm3.toString(), PROVIDER);
        // 初始化为签名状态
        signature.initSign(bcecPrivateKey);
        // 传入签名字节
        signature.update(plainText.getBytes(StandardCharsets.UTF_8));
        // 签名
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 验签
     *
     * @param plainText 待签名文本
     * @param signText  签名(base64)
     * @param publicKey 公钥文本，hex 裸公钥点或 Base64(X509)，自动识别
     * @return 验签是否通过
     * @throws GeneralSecurityException
     */
    public static boolean verify(String plainText, String signText, String publicKey) throws Exception {
        BCECPublicKey pk = (BCECPublicKey) createPublicKey(publicKey);
        return verify(plainText, signText, pk.getQ().getEncoded(false));
    }

    public static boolean verify(String plainText, String signText, byte[] publicKey) throws GeneralSecurityException {
        X9ECParameters parameters = GMNamedCurves.getByOID(GMObjectIdentifiers.sm2p256v1);
        ECParameterSpec parameterSpec = new ECParameterSpec(parameters.getCurve(), parameters.getG(), parameters.getN());
        ECPoint ecPoint = parameters.getCurve().decodePoint(publicKey);
        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecPoint, parameterSpec);
        PublicKey bcecPublicKey = new BCECPublicKey("EC", publicKeySpec, BouncyCastleProvider.CONFIGURATION);
        // 创建签名对象
        Signature signature = Signature.getInstance(GMObjectIdentifiers.sm2sign_with_sm3.toString(), PROVIDER);
        // 初始化为验签状态
        signature.initVerify(bcecPublicKey);
        signature.update(plainText.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(signText));
    }
}