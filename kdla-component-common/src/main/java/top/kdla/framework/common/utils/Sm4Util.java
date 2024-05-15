/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Objects;

/**
 * 国密SM4分组密码算法工具类（对称加密）,用于替代DES/AES等国际算法
 * <pre>
 *     SM4为无线局域网标准的分组加密算法，对称加密，用于替代DES/AES等国际算法，于2012年3月21日发布，
 *     SM4算法与 AES算法具有相同的密钥长度和分组长度，均为128位，故对消息进行加解密时，若消息长度过长，需要进行分组，要消息长度不足，则要进行填充。
 *     加密算法与密钥扩展算法都采用32轮非线性迭代结构，解密算法与加密算法的结构相同，只是轮密钥的使用顺序相反，解密轮密钥是加密轮密钥的逆序
 * </pre>
 *
 * @author wangMaoXiong
 * @version 1.0
 * @date 2024/4/14 8:03
 */
public class Sm4Util {
    private static final String ALGORITHM = "SM4";
    private static final String ALGORITHM_ECB_PKCS5PADDING = "SM4/ECB/PKCS5Padding";
    private static final String CHARSET_UTF8 = "utf-8";
    /**
     * SM4算法目前只支持128位（即密钥16字节）
     */
    private static final int DEFAULT_KEY_SIZE = 128;
    private static final Logger log = LoggerFactory.getLogger(Sm4Util.class);

    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

    static {
        // 防止内存中出现多次BouncyCastleProvider的实例
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(PROVIDER);
        }
    }

    /**
     * 生成密钥文本
     *
     * @param keySrc — 用于生成密钥文本的随机值，值一致，生成的密钥文本就一致。
     * @return :密钥文本，用于后续进行加解密
     */
    public static String generateKey(String keySrc) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        SecureRandom random = new SecureRandom(keySrc.getBytes(CHARSET_UTF8));
        keyGen.init(DEFAULT_KEY_SIZE, random);
        SecretKey secretKey = keyGen.generateKey();
        String secretKeyText = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        return secretKeyText;
    }

    /**
     * Sm4 加密
     *
     * @param data          ：被加密的数据
     * @param secretKeyText ：密钥文本
     * @return ：加密好的密文(base64)
     * @throws Exception
     */
    public static String encrypt(String data, String secretKeyText) throws Exception {
       return encrypt(data,Hex.decode(secretKeyText));
    }

    public static String encrypt(String data, byte[] secretKeyText) throws Exception {
        SecretKey secretKey = new SecretKeySpec(secretKeyText, ALGORITHM);
        byte[] dataBytes = data.getBytes(CHARSET_UTF8);
        byte[] encryptBytes = encryptMode(secretKey, dataBytes, Cipher.ENCRYPT_MODE);
        return Base64.getEncoder().encodeToString(encryptBytes);
    }

    /**
     * 解密
     *
     * @param cipherText    ：密文(base64)
     * @param secretKeyText :密钥文本
     * @return ：明文
     */
    public static String decrypt(String cipherText, String secretKeyText) throws Exception {
        return decrypt(cipherText,Hex.decode(secretKeyText));
    }

    public static String decrypt(String cipherText, byte[] secretKeyText) throws Exception {
        SecretKey secretKey = new SecretKeySpec(secretKeyText, ALGORITHM);
        byte[] cipherTextBytes = Base64.getDecoder().decode(cipherText);
        byte[] decryptBytes = encryptMode(secretKey, cipherTextBytes, Cipher.DECRYPT_MODE);
        return new String(decryptBytes, CHARSET_UTF8);
    }

    private static SecretKey strKeyToSecretKey(String strKey) {
        byte[] bytes = Base64.getDecoder().decode(strKey);
        SecretKeySpec secretKey = new SecretKeySpec(bytes, ALGORITHM);
        return secretKey;
    }

    private static byte[] encryptMode(SecretKey desKey, byte[] src, int mode) throws Exception {
        // 加密
        Cipher cipher = Cipher.getInstance(ALGORITHM_ECB_PKCS5PADDING, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(mode, desKey);
        byte[] enc = cipher.doFinal(src);
        return enc;
    }

}
