package top.kdla.framework.common.utils;

import java.nio.charset.StandardCharsets;
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
public class AESUtils {

    private static final int PASSWORD_LENGTH = 16;
    private static final String AES_PATH = "AES/CBC/PKCS5Padding";

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
            throw new RuntimeException("Invalid key size. " + password + ", length no 16 size");
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance(AES_PATH);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(new byte[PASSWORD_LENGTH])); // zero IV
            byte[] finalCode = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(finalCode);
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
            throw new RuntimeException("Invalid key size. " + password + ", length no 16 size");
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance(AES_PATH);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(new byte[PASSWORD_LENGTH]));
            byte[] toDecrypt = Base64.getDecoder().decode(content.getBytes());
            byte[] original = cipher.doFinal(toDecrypt);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("decrypt exception", e);
        }
    }

}