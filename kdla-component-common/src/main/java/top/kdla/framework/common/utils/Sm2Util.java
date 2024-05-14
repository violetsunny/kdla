/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ECParameters;
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
     * 生成国密公私钥对
     *
     * @param keySrc ：生成公私密钥的随机种子，可以任意随机。
     * @return ：返回公公私密钥(base64)，[0] 公钥、[1] 私钥
     * @throws Exception
     */
    public static String[] generateKeyPair(String keySrc) throws Exception {
        SecureRandom secureRandom = new SecureRandom(keySrc.getBytes(CHARSET_UTF8));
        ECGenParameterSpec sm2Spec = new ECGenParameterSpec(STD_NAME);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM, new BouncyCastleProvider());
        keyPairGenerator.initialize(sm2Spec);
        keyPairGenerator.initialize(sm2Spec, secureRandom);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        String publicKeyText = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
        String privateKeyText = new String(Base64.getEncoder().encode(privateKey.getEncoded()));
        // String[0] 公钥
        // String[1] 私钥
        String[] result = {publicKeyText, privateKeyText};
        return result;
    }

    /**
     * 公钥加密
     *
     * @param data          ：被加密的数据
     * @param publicKeyText ：公钥key文本
     * @return ：加密好的密文(base64)
     */
    public static String encrypt(String data, String publicKeyText) throws Exception {
        PublicKey publicKey = createPublicKey(publicKeyText);
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        ECPublicKeyParameters localECPublicKeyParameters = null;

        if (publicKey instanceof BCECPublicKey) {
            BCECPublicKey localECPublicKey = (BCECPublicKey) publicKey;
            ECParameterSpec localECParameterSpec = localECPublicKey.getParameters();
            ECDomainParameters localECDomainParameters = new ECDomainParameters(localECParameterSpec.getCurve(),
                    localECParameterSpec.getG(), localECParameterSpec.getN());
            localECPublicKeyParameters = new ECPublicKeyParameters(localECPublicKey.getQ(), localECDomainParameters);
        }
        SM2Engine localSM2Engine = new SM2Engine();
        localSM2Engine.init(true, new ParametersWithRandom(localECPublicKeyParameters, new SecureRandom()));
        byte[] arrayOfByte2 = localSM2Engine.processBlock(bytes, 0, bytes.length);
        return Base64.getEncoder().encodeToString(arrayOfByte2);
    }

    /**
     * 私钥解密
     *
     * @param data           ：密文(base64)
     * @param privateKeyText ：私钥文本
     * @return ：明文
     */
    public static String decrypt(String data, String privateKeyText) throws Exception {
        PrivateKey privateKey = createPrivateKey(privateKeyText);
        byte[] encodeData = Base64.getDecoder().decode(data);
        SM2Engine localSM2Engine = new SM2Engine();
        BCECPrivateKey sm2PriK = (BCECPrivateKey) privateKey;
        ECParameterSpec localECParameterSpec = sm2PriK.getParameters();
        ECDomainParameters localECDomainParameters = new ECDomainParameters(localECParameterSpec.getCurve(), localECParameterSpec.getG(), localECParameterSpec.getN());
        ECPrivateKeyParameters localECPrivateKeyParameters = new ECPrivateKeyParameters(sm2PriK.getD(), localECDomainParameters);
        localSM2Engine.init(false, localECPrivateKeyParameters);
        byte[] arrayOfByte3 = localSM2Engine.processBlock(encodeData, 0, encodeData.length);
        return new String(arrayOfByte3);
    }

    /**
     * 将Base64转码的公钥串，转化为公钥对象
     *
     * @param publicKeyText
     * @return
     */
    private static PublicKey createPublicKey(String publicKeyText) throws Exception {
        byte[] decode = Base64.getDecoder().decode(publicKeyText);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decode);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, new BouncyCastleProvider());
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        return publicKey;
    }

    /**
     * 将Base64转码的私钥串，转化为私钥对象
     *
     * @param privateKeyText
     * @return
     */
    private static PrivateKey createPrivateKey(String privateKeyText) throws Exception {
        byte[] decode = Base64.getDecoder().decode(privateKeyText);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(decode);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, new BouncyCastleProvider());
        PrivateKey publicKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        return publicKey;
    }

    /**
     * 签名
     *
     * @param plainText  待签名文本
     * @param privateKey 私钥
     * @return
     * @throws GeneralSecurityException
     */
    public static String sign(String plainText, String privateKey) throws Exception {
        return sign(plainText,new BigInteger(Hex.decode(privateKey)));
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
     * @param signText  签名
     * @param publicKey 公钥
     * @return
     * @throws GeneralSecurityException
     */
    public static boolean verify(String plainText, String signText, String publicKey) throws Exception {
        return verify(plainText,signText,Hex.decode(publicKey));
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
