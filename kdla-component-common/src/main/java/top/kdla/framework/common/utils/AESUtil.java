package top.kdla.framework.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加解密
 * 
 * @author kll
 * @version $Id: AESUtils $
 */
public class AESUtil {

    private static final int PASSWORD_LENGTH = 16;
    private static final String AES_PATH = "AES/CBC/PKCS5Padding";
    private static final String V1_PREFIX = "v1:";

    /**
     * 加密
     *
     * @param content
     *            需要加密的内容
     * @param password
     *            加密密码
     * @return
     */
    public static String encrypt(String content, String password) {
        byte[] raw = password.getBytes(StandardCharsets.UTF_8);
        if (raw.length != PASSWORD_LENGTH) {
            // 安全红线：异常消息禁止携带密钥明文，仅保留长度信息
            throw new RuntimeException("Invalid key size, length " + raw.length + " no 16 size");
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
            byte[] iv = new byte[PASSWORD_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_PATH);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] finalCode = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + finalCode.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(finalCode, 0, combined, iv.length, finalCode.length);
            return V1_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("encrypt exception", e);
        }
    }

    /**
     * 解密
     *
     * @param content
     *            需要解密的内容
     * @param password
     *            加密密码
     * @return
     */
    public static String decrypt(String content, String password) {
        byte[] raw = password.getBytes(StandardCharsets.UTF_8);
        if (raw.length != PASSWORD_LENGTH) {
            // 安全红线：异常消息禁止携带密钥明文，仅保留长度信息
            throw new RuntimeException("Invalid key size, length " + raw.length + " no 16 size");
        }
        SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
        if (content.startsWith(V1_PREFIX)) {
            byte[] combined = Base64.getDecoder().decode(content.substring(V1_PREFIX.length()).getBytes());
            try {
                byte[] iv = new byte[PASSWORD_LENGTH];
                byte[] toDecrypt = new byte[combined.length - PASSWORD_LENGTH];
                System.arraycopy(combined, 0, iv, 0, PASSWORD_LENGTH);
                System.arraycopy(combined, PASSWORD_LENGTH, toDecrypt, 0, toDecrypt.length);
                Cipher cipher = Cipher.getInstance(AES_PATH);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
                byte[] original = cipher.doFinal(toDecrypt);
                return new String(original, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("decrypt exception", e);
            }
        }
        byte[] combined = Base64.getDecoder().decode(content.getBytes());
        try {
            byte[] zeroIv = new byte[PASSWORD_LENGTH];
            Cipher cipher = Cipher.getInstance(AES_PATH);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(zeroIv));
            byte[] original = cipher.doFinal(combined);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("decrypt exception", e);
        }
    }

}