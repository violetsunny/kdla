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
    public static Pattern DATETIME_WITH_MILLISECOND_1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 日期时间格式 yyyy/MM/dd HH:mm:ss.SSS
     */
    public static Pattern DATETIME_WITH_MILLISECOND_2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 日期时间格式 yyyy-MM-ddTHH:mm:ss.SSS
     */
    public static Pattern DATETIME_WITH_MILLISECOND_3 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 小数的正则表达式
     */
    public static Pattern DOUBLE = Pattern.compile("[+-]?[0-9]+(\\.[0-9]+)?");

    /**
     * 整数的正则表达式
     */
    public static Pattern INTEGER = Pattern.compile("^-?\\d+$");

    /**
     * 数字的正则表达式
     */
    public static Pattern NUMBER = Pattern.compile("[0-9]*");

    /**
     * 科学计数法的正则表达式
     */
    public static Pattern SCIENTIFIC_NOTATION = Pattern.compile("[+-]?\\d+(\\.\\d+)?([Ee][+-]?\\d+)?");

    /**
     * CJK统一汉字
     */
    public static Pattern CHINESE_REG = Pattern.compile("[\\u4E00-\\u9FBF]+");

    /**
     * CJK统一汉字
     */
    public static Pattern CHINESE_NAME = Pattern.compile("\\p{InCJK Unified Ideographs}&&\\P{Cn}");

    /**
     * 判断是否为小数
     * 如果你只需要找到输入序列中的任何位置的匹配，可以使用m.find()。
     * 如果你需要整个输入序列完全匹配模式，可以使用m.matches()。
     *
     * @param str
     * @return
     */
    public static boolean validateFloatNumber(String str) {
        return DOUBLE.matcher(str).matches();
    }

    /**
     * 判断是否有汉字
     * 如果你只需要找到输入序列中的任何位置的匹配，可以使用m.find()。
     * 如果你需要整个输入序列完全匹配模式，可以使用m.matches()。
     *
     * @param str
     * @return
     */
    public static boolean validateChinese(String str) {
        return CHINESE_REG.matcher(str).find();
    }

}
