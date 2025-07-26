/**
 * llkang.com Inc.
 * Copyright (c) 2014-2025 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Bcrypt密码哈希工具类
 * @author kanglele
 * @version $Id: BCryptUtil.java, v 0.1 2025/7/22 16:00 kanglele Exp $
 */
public class BCryptUtil {
    /**
     * 默认工作因子（12轮哈希迭代）
     * 值越大计算越慢，安全性越高，建议值：10-14
     */
    private static final int DEFAULT_WORKLOAD = 12;

    /**
     * 生成盐值并哈希密码
     * @param password 明文密码
     * @return 加密后的哈希值（包含盐值信息）
     */
    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(DEFAULT_WORKLOAD));
    }

    /**
     * 使用指定盐值哈希密码
     * @param password 明文密码
     * @param salt 盐值（必须是BCrypt格式）
     * @return 加密后的哈希值
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("密码和盐值都不能为空");
        }
        return BCrypt.hashpw(password, salt);
    }

    /**
     * 验证密码是否匹配哈希值
     * @param password 明文密码
     * @param hashedPassword 存储的哈希值
     * @return 是否匹配
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(password, hashedPassword);
    }

    /**
     * 获取新的盐值
     * @return BCrypt格式的盐值字符串
     */
    public static String generateSalt() {
        return BCrypt.gensalt(DEFAULT_WORKLOAD);
    }
}
