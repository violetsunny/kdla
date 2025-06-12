package top.kdla.framework.supplement.trdcloud.utils;

import org.apache.commons.collections4.MapUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtil {
    private static final Pattern p = Pattern.compile("(\\$\\{)([\\w]+)(\\})");
    // 定义一个匹配中文字符的正则表达式
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");

    public static boolean validateChinese(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return CHINESE_PATTERN.matcher(input).find();
    }

    public static String replaceUrl(String url, Map<String, Object> map) {
        if (MapUtils.isEmpty(map)) {
            return url;
        }

        Matcher m = p.matcher(url);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String group = m.group(2);//规则中${值}中的值替换
            Object v = map.get(group);
            if (v != null) {
                String pram = String.valueOf(v);
                try {
                    pram = URLEncoder.encode(pram, "UTF-8");
                } catch (Exception e) {

                }
                m.appendReplacement(sb, pram);
            }

        }
        //把符合的数据追加到sb尾
        m.appendTail(sb);
        return sb.toString();
    }

    public static Map<String, String> transform(Map<String, Object> map) {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    }

    public static Map<String, Object> transform2(Map<String, String> map) {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @param message
     * @param algorithm MD5-32位，SHA-256 -64位
     * @return
     */
    public static String hashString(String message, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(message.getBytes(StandardCharsets.UTF_8));
            return convertByteArrayToHexString(hashedBytes);
        } catch (Exception e) {
            return message;
        }
    }

    public static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte bytes : arrayBytes) {
            stringBuffer.append(Integer.toString((bytes & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }
}
