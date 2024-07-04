/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author kanglele
 * @version $Id: SHAUtil, v 0.1 2024/7/4 14:19 kanglele Exp $
 */
public class SHAUtil {

    /**
     * 可以用
     * DigestUtils.sha1Hex();
     * DigestUtil.sha1Hex();
     * @param plainText
     * @return
     */
    public static String getSHA1(String plainText) {
        return getSHA(plainText, "sha-1", false);
    }

    public static String getSHA256(String plainText) {
        return getSHA(plainText, "sha-256", false);
    }
    public static String getSHA224(String plainText) {
        return getSHA(plainText, "sha-224", false);
    }

    public static String getSHA384(String plainText) {
        return getSHA(plainText, "sha-384", false);
    }

    public static String getSHA512(String plainText) {
        return getSHA(plainText, "sha-512", false);
    }

    /**
     * 可以用
     * DigestUtils.md5Hex();
     * DigestUtil.md5Hex();
     * @param plainText
     * @return
     */
    public static String getMD5(String plainText) {
        return getSHA(plainText, "md5", false);
    }

    public static String getSHA1(String plainText, boolean upperCase) {
        return getSHA(plainText, "sha-1", upperCase);
    }

    public static String getSHA256(String plainText, boolean upperCase) {
        return getSHA(plainText, "sha-256", upperCase);
    }
    public static String getSHA224(String plainText, boolean upperCase) {
        return getSHA(plainText, "sha-224", upperCase);
    }

    public static String getSHA384(String plainText, boolean upperCase) {
        return getSHA(plainText, "sha-384", upperCase);
    }

    public static String getSHA512(String plainText, boolean upperCase) {
        return getSHA(plainText, "sha-512", upperCase);
    }

    public static String getMD5(String plainText, boolean upperCase) {
        return getSHA(plainText, "md5", upperCase);
    }

    /**
     * 利用 Java 原生摘要实现 SHA 或 MD5 加密（支持大小写，默认小写）
     *
     * @param plainText 要加密的数据
     * @param algorithm 要使用的算法（如 SHA-1、SHA-256、MD5 等）
     * @param upperCase 是否转为大写
     * @return
     */
    private static String getSHA(String plainText, String algorithm, boolean upperCase) {
        // 将输入的字符串转换为字节数组
        byte[] bytes = plainText.getBytes(StandardCharsets.UTF_8);
        MessageDigest messageDigest;
        try {
            // 获取指定算法的 MessageDigest 对象
            messageDigest = MessageDigest.getInstance(algorithm);
            // 更新摘要信息
            messageDigest.update(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("摘要签名过程中出现错误, 算法异常");
        }
        // 计算并返回摘要结果，也是字节数组
        byte[] digest = messageDigest.digest();
        // 将字节数组转换为十六进制字符串
        String result = byteArrayToHexString(digest);
        // 根据需要转换为大写
        return upperCase? result.toUpperCase() : result;
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 要转换的字节数组
     * @return
     */
    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            // 使用 & 0xff 操作将高 24 位置 0，避免错误
            String temp = Integer.toHexString(b & 0xff);
            if (temp.length() == 1) {
                // 对于只有一位的十六进制数，进行补 0 操作
                builder.append("0");
            }
            builder.append(temp);
        }
        return builder.toString();
    }
}
