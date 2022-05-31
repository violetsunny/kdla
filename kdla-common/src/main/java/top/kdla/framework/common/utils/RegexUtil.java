package top.kdla.framework.common.utils;

import java.util.regex.Matcher;
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
    public static Pattern XmlReplacePattern = Pattern.compile("((<!\\[CDATA\\[[\\s\\S]*\\]\\]>)|(&)|([\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]))");

    /**
     * 替换替换非法干扰字符，例如：<list xmlns="http://www.yundaex.com/schema/2013">
     */
    public static Pattern XmlnsReplacePattern = Pattern.compile(" xmlns[^>]*\\>");

    /**
     * 清理非法字符
     */
    public static Pattern ClearAddressPattern = Pattern.compile("[，|,|;|；]");

    /**
     * 清理非法字符
     */
    public static Pattern ClearXmlPattern = Pattern.compile("<\\?.*\\?>\\n");

    /**
     * 日期格式 yyyy-MM-dd
     */
    private static Pattern DatePattern1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    /**
     * 日期格式yyyy/MM/dd
     */
    private static Pattern DatePattern2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2}$");

    /**
     * 日期时间格式 yyyy-MM-dd HH:mm:ss
     */
    private static Pattern DateTimePattern1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    /**
     * 日期时间格式 yyyy/MM/dd HH:mm:ss
     */
    private static Pattern DateTimePattern2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    /**
     * 日期时间格式 yyyy:MM:dd HH:mm:ss.SSS
     */
    private static Pattern DatetimeWithMillisecondPattern1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 日期时间格式 yyyy/MM/dd HH:mm:ss.SSS
     */
    private static Pattern DatetimeWithMillisecondPattern2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 小数的正则表达式
     */
    private static final Pattern PATTERN_FLOAT = Pattern.compile("[+-]?[0-9]+(\\.[0-9]+)?");

    /**
     * 判断是否是 日期格式 yyyy-MM-dd
     * @param input
     * @return
     */
    public static Boolean isDate1(String input) {
        return DatePattern1.matcher(input).matches();
    }

    /**
     * 判断是否是 日期格式yyyy/MM/dd
     * @param input
     * @return
     */
    public static Boolean isDate2(String input) {
        return DatePattern2.matcher(input).matches();
    }

    /**
     * 判断是否是 日期时间格式 yyyy-MM-dd HH:mm:ss
     * @param input
     * @return
     */
    public static Boolean isDateTime1(String input) {
        return DateTimePattern1.matcher(input).matches();
    }

    /**
     * 判断是否是 日期时间格式 yyyy/MM/dd HH:mm:ss
     * @param input
     * @return
     */
    public static Boolean isDateTime2(String input) {
        return DateTimePattern2.matcher(input).matches();
    }

    /**
     * 判断是否是 日期时间格式 yyyy-MM-dd HH:mm:ss.SSS
     * @param input
     * @return
     */
    public static Boolean isDatetimeWithMillisecond1(String input) {
        return DatetimeWithMillisecondPattern1.matcher(input).matches();
    }

    /**
     * 判断是否是 日期时间格式 yyyy/MM/dd HH:mm:ss.SSS
     * @param input
     * @return
     */
    public static Boolean isDatetimeWithMillisecond2(String input) {
        return DatetimeWithMillisecondPattern2.matcher(input).matches();
    }

    /**
     * 判断是否为小数
     *
     * @param str
     * @return
     */
    public static boolean validateFloatNumber(String str) {
        Matcher isNum = PATTERN_FLOAT.matcher(str);
        return isNum.matches();
    }
}
