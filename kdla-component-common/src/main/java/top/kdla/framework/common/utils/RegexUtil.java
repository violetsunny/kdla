package top.kdla.framework.common.utils;

import java.util.regex.Pattern;

/**
 * 正则工具类
 *
 * @author kll
 * @since 2021-07-12
 */
public class RegexUtil {

    /**
     * 过滤非法字符正则
     * 1.过滤掉定界符
     * 2.过滤掉&符号
     * 3.过滤掉表情符号
     */
    public static Pattern XML_REPLACE_PATTERN = Pattern.compile("((<!\\[CDATA\\[[\\s\\S]*\\]\\]>)|(&)|([\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]))");

    /**
     * 替换替换非法干扰字符，例如：<list xmlns="http://www.yundaex.com/schema/2013">
     */
    public static Pattern XMLNS_REPLACE_PATTERN = Pattern.compile(" xmlns[^>]*\\>");

    /**
     * 清理非法字符
     */
    public static Pattern CLEAR_ADDRESS_PATTERN = Pattern.compile("[，|,|;|；]");

    /**
     * 清理非法字符
     */
    public static Pattern CLEAR_XML_PATTERN = Pattern.compile("<\\?.*\\?>\\n");

    /**
     * 日期格式 yyyy-MM-dd
     */
    public static Pattern DATE_PATTERN_1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    /**
     * 日期格式yyyy/MM/dd
     */
    public static Pattern DATE_PATTERN_2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2}$");

    /**
     * 日期时间格式 yyyy-MM-dd HH:mm:ss
     */
    public static Pattern DATE_TIME_PATTERN_1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    /**
     * 日期时间格式 yyyy/MM/dd HH:mm:ss
     */
    public static Pattern DATE_TIME_PATTERN_2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    /**
     * 日期时间格式 yyyy-MM-dd HH:mm:ss.SSS
     */
    public static Pattern DATETIME_WITH_MILLISECOND_PATTERN_1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 日期时间格式 yyyy/MM/dd HH:mm:ss.SSS
     */
    public static Pattern DATETIME_WITH_MILLISECOND_PATTERN_2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 小数的正则表达式
     */
    public static Pattern PATTERN_FLOAT = Pattern.compile("[+-]?[0-9]+(\\.[0-9]+)?");

    /**
     * 数字的正则表达式
     */
    public static Pattern NUMBER = Pattern.compile("[0-9]*");

    /**
     * CJK统一汉字
     */
    public static Pattern CHINESE_REG = Pattern.compile("[\\u4E00-\\u9FBF]+");

    /**
     * CJK统一汉字
     */
    public static Pattern CHINESE_NAME = Pattern.compile("\\p{InCJK Unified Ideographs}&amp;&amp;\\P{Cn}");

    /**
     * 判断是否为小数
     *
     * @param str
     * @return
     */
    public static boolean validateFloatNumber(String str) {
        return PATTERN_FLOAT.matcher(str).matches();
    }
}
