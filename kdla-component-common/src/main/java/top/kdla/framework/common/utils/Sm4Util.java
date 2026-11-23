/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
    private static final String ALGORITHM_CBC_PKCS5PADDING = "SM4/CBC/PKCS5Padding";
    private static final String CHARSET_UTF8 = "utf-8";
    /**
     * SM4算法目前只支持128位（即密钥16字节）
     */
    private static final int DEFAULT_KEY_SIZE = 128;
    /**
     * CBC模式IV长度（128位分组），与密钥长度一致
     */
    private static final int IV_LENGTH = 16;
    /**
     * 新密文格式前缀，对齐AESUtil既有约定：v1:Base64(IV+密文)；无前缀为旧ECB格式
     */
    private static final String V1_PREFIX = "v1:";
    private static final Logger log = LoggerFactory.getLogger(Sm4Util.class);

    private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

    static {
        // 防止内存中出现多次BouncyCastleProvider的实例
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(PROVIDER);
        }
    }

    /**
     * 生成密钥文本（真随机）。
     * <p>生产密钥优先使用本方法，密钥文本仅生成一次并经密钥管理设施存储分发。
     *
     * @return :密钥文本，用于后续进行加解密
     */
    public static String generateSecureKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        keyGen.init(DEFAULT_KEY_SIZE);
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * 生成密钥文本
     *
     * @param keySrc — 用于生成密钥文本的随机值，值一致，生成的密钥文本就一致。
     * @return :密钥文本，用于后续进行加解密
     * @deprecated 低熵种子播种SecureRandom，同keySrc必派生同密钥，keySrc可猜测即密文全破。
     *             仅为兼容既有数据保留，新场景使用 {@link #generateSecureKey()}
     */
    @Deprecated
    public static String generateKey(String keySrc) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        SecureRandom random = new SecureRandom(keySrc.getBytes(CHARSET_UTF8));
        keyGen.init(DEFAULT_KEY_SIZE, random);
        SecretKey secretKey = keyGen.generateKey();
        String secretKeyText = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        return secretKeyText;
    }

    /**
     * Sm4 加密（v1格式：CBC+随机IV，输出 v1:Base64(IV+密文)）
     *
     * @param data          ：被加密的数据
     * @param secretKeyText ：密钥文本
     * @return ：加密好的密文
     * @throws Exception
     */
    public static String encrypt(String data, String secretKeyText) throws Exception {
        return encryptV1(data.getBytes(CHARSET_UTF8), strKeyToSecretKey(secretKeyText));
    }

    public static String encrypt(String data, byte[] secretKeyText) throws Exception {
        return encryptV1(data.getBytes(CHARSET_UTF8), new SecretKeySpec(secretKeyText, ALGORITHM));
    }

    /**
     * 解密（带v1前缀走CBC+随机IV；无前缀为旧ECB格式兼容分支）
     *
     * @param cipherText    ：密文
     * @param secretKeyText :密钥文本
     * @return ：明文
     */
    public static String decrypt(String cipherText, String secretKeyText) throws Exception {
        return decryptCompat(cipherText, strKeyToSecretKey(secretKeyText));
    }

    public static String decrypt(String cipherText, byte[] secretKeyText) throws Exception {
        return decryptCompat(cipherText, new SecretKeySpec(secretKeyText, ALGORITHM));
    }

    private static String encryptV1(byte[] dataBytes, SecretKey secretKey) throws Exception {
        // ECB相同明文块产生相同密文块泄露数据模式，改CBC+随机IV，格式对齐AESUtil的v1约定
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM_CBC_PKCS5PADDING, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] enc = cipher.doFinal(dataBytes);
        byte[] combined = new byte[iv.length + enc.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(enc, 0, combined, iv.length, enc.length);
        return V1_PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    private static String decryptCompat(String cipherText, SecretKey secretKey) throws Exception {
        if (cipherText != null && cipherText.startsWith(V1_PREFIX)) {
            byte[] combined = Base64.getDecoder().decode(cipherText.substring(V1_PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] toDecrypt = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, toDecrypt, 0, toDecrypt.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM_CBC_PKCS5PADDING, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(toDecrypt), CHARSET_UTF8);
        }
        // 旧ECB格式兼容分支：解密存量密文；下游数据迁移完成后可在2.x移除
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
