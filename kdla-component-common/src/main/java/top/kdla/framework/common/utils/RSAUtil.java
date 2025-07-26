/**
 * llkang.com Inc.
 * Copyright (c) 2014-2025 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * @author kanglele
 * @version $Id: RSAUtil, v 0.1 2025/7/22 14:26 kanglele Exp $
 */
public class RSAUtil {
    // RSA 算法名称
    private static final String ALGORITHM = "RSA";
    // RSA 密钥长度
    private static final int KEY_SIZE = 2048;

    /**
     * 生成 RSA 密钥对
     * @return 包含公钥和私钥的 KeyPair 对象
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 使用公钥进行 RSA 加密
     * @param plainText 待加密的明文
     * @param publicKey 公钥
     * @return 加密后的 Base64 编码字符串
     */
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 使用私钥进行 RSA 解密
     * @param encryptedText 加密后的 Base64 编码字符串
     * @param privateKey 私钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 将 Base64 编码的公钥字符串转换为 PublicKey 对象
     * @param publicKeyBase64 Base64 编码的公钥字符串
     * @return PublicKey 对象
     */
    public static PublicKey getPublicKey(String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 将 Base64 编码的私钥字符串转换为 PrivateKey 对象
     * @param privateKeyBase64 Base64 编码的私钥字符串
     * @return PrivateKey 对象
     */
    public static PrivateKey getPrivateKey(String privateKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
}
