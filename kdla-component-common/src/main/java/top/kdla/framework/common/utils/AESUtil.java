package top.kdla.framework.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加解密。密文格式按前缀区分：
 * <pre>
 *   v2:Base64(12字节nonce + GCM密文含16字节tag) —— 当前加密输出，AEAD 带完整性校验
 *   v1:Base64(16字节IV + CBC密文)              —— 存量兼容，仅解密
 *   无前缀 Base64(CBC密文)                      —— 旧全零IV存量兼容，仅解密
 * </pre>
 *
 * @author kll
 * @version $Id: AESUtils $
 */
public class AESUtil {

    private static final int PASSWORD_LENGTH = 16;
    /** CBC 仅用于解密 v1 与旧全零IV存量密文 */
    private static final String AES_PATH = "AES/CBC/PKCS5Padding";
    private static final String AES_GCM_PATH = "AES/GCM/NoPadding";
    private static final String V1_PREFIX = "v1:";
    private static final String V2_PREFIX = "v2:";
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    /**
     * 加密（v2格式：GCM+随机nonce，输出 v2:Base64(nonce+密文含tag)）
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
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(AES_GCM_PATH);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] finalCode = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[nonce.length + finalCode.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(finalCode, 0, combined, nonce.length, finalCode.length);
            return V2_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("encrypt exception", e);
        }
    }

    /**
     * 解密（v2走GCM；v1走CBC+随机IV；无前缀走旧全零IV CBC兼容分支）
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
        if (content.startsWith(V2_PREFIX)) {
            byte[] combined = Base64.getDecoder().decode(content.substring(V2_PREFIX.length()));
            try {
                byte[] nonce = new byte[GCM_NONCE_LENGTH];
                byte[] toDecrypt = new byte[combined.length - GCM_NONCE_LENGTH];
                System.arraycopy(combined, 0, nonce, 0, GCM_NONCE_LENGTH);
                System.arraycopy(combined, GCM_NONCE_LENGTH, toDecrypt, 0, toDecrypt.length);
                Cipher cipher = Cipher.getInstance(AES_GCM_PATH);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
                return new String(cipher.doFinal(toDecrypt), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("decrypt exception", e);
            }
        }
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