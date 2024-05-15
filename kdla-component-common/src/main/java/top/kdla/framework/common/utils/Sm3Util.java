/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import cn.hutool.core.util.HexUtil;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * 国密算法 - SM3加密工具  杂凑算法 用来替换MD5、SHA-1、SHA-2
 * <pre>
 *     SM3为密码杂凑算法，采用密码散列(hash)）函数标准，用于替代MD5/SHA-1/SHA-2等国际算法，于2010年12月17日发布。
 *     是在 SHA-256基础上改进实现的一种算法，消息分组长度为512位，摘要值长度为256位，
 *     其中使用了异或、模、模加、移位、与、或、非运算，由填充、迭代过程、消息扩展和压缩函数所构成。
 *     在商用密码体系中，SM3主要用于数字签名及验证、消息认证码生成及验证、随机数生成等。
 *     据国家密码管理局表示，其安全性及效率要高于MD5算法和SHA-1算法，与SHA-256相当。
 * </pre>
 *
 * @author wangMaoXiong
 * @version 1.0
 * @date 2024/4/13 15:23
 **/
public class Sm3Util {

    private static final String ENCODING = "UTF-8";
    private static final Logger log = LoggerFactory.getLogger(Sm3Util.class);

    /**
     * 通过密钥进行加密
     *
     * @param key     密钥
     * @param srcData 被加密的byte数组
     * @return
     * @explain 指定密钥进行加密
     */
    public static byte[] hmac(byte[] key, byte[] srcData) {
        KeyParameter keyParameter = new KeyParameter(key);
        SM3Digest digest = new SM3Digest();
        HMac mac = new HMac(digest);
        mac.init(keyParameter);
        mac.update(srcData, 0, srcData.length);
        byte[] result = new byte[mac.getMacSize()];
        mac.doFinal(result, 0);
        return result;
    }

    public static String encrypt(String data, String key) {
        // 将返回的hash值转换成16进制字符串
        String resultHexString = "";
        try {
            // 将字符串转换成byte数组
            byte[] srcData = data.getBytes(ENCODING);
            // 调用hash()
            byte[] resultHash = hmac(key.getBytes(ENCODING), srcData);
            // 将返回的hash值转换成16进制字符串
            resultHexString = HexUtil.encodeHexStr(resultHash);
        } catch (UnsupportedEncodingException e) {
            log.error("加密异常", e);
        }
        return resultHexString;
    }

    /**
     * 判断源数据与加密数据是否一致
     *
     * @param srcStr       原字符串
     * @param keyStr       密钥
     * @param sm3HexString 16进制字符串
     * @return 校验结果
     * @explain 通过验证原数组和生成的hash数组是否为同一数组，验证2者是否为同一数据
     */
    public static boolean verify(String srcStr, String keyStr, String sm3HexString) {
        boolean flag = false;
        try {
            byte[] srcData = srcStr.getBytes(ENCODING);
            byte[] keyData = keyStr.getBytes(ENCODING);
            byte[] sm3Hash = HexUtil.decodeHex(sm3HexString);
            byte[] newHash = hmac(keyData, srcData);
            if (Arrays.equals(newHash, sm3Hash)) {
                flag = true;
            }
        } catch (UnsupportedEncodingException e) {
            log.error("验证sm3加密结果异常", e);

        }
        return flag;
    }

}
