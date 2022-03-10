package top.kdla.framework.common.constants;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 时间格式的常量
 * @author vincent.li
 */
public class DateTimeFormat {

    /**
     * 日期格式yyyy-MM-dd
     */
    public static final String DATE1 = "yyyy-MM-dd";
    /**
     * 日期格式yyyy/MM/dd
     */
    public static final String DATE2 = "yyyy/MM/dd";

    /**
     * 日期格式yyyyMMdd
     */
    public static final String DATE3 = "yyyyMMdd";

    /**
     * 日期格式yyMMdd
     */
    public static final String FORMAT_YYMMDD = "yyMMdd";


    /**
     * 日期小时格式yyMMddHH
     */
    public static final String FORMAT_YYMMDDHH = "yyMMddHH";

    /**
     * 日期时间yyyy-MM-dd HH:mm:ss
     */
    public static final String DATETIME1 = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期时间yyyy/MM/dd HH:mm:ss
     */
    public static final String DATETIME2 = "yyyy/MM/dd HH:mm:ss";
    /**
     * 日期时间yyyy-MM-dd HH:mm:ss.SSS
     */
    public static final String DATETIMEWITHMILLISECOND1 = "yyyy-MM-dd HH:mm:ss.SSS";
    /**
     * 日期时间yyyy/MM/dd HH:mm:ss.SSS
     */
    public static final String DATETIMEWITHMILLISECOND2 = "yyyy/MM/dd HH:mm:ss.SSS";

    /**
     * 日期格式yyyyMMddHHmmssSSS
     */
    public static final String DATETIMESTAMP = "yyyyMMddHHmmssSSS";

    /**
     * es 日期格式
     */
    public static final String ES_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    /**
     * es 日期格式
     */
    public static final DateTimeFormatter ES_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.CHINA);

    /**
     * 日期格式yyyyMMdd
     */
    public static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    /**
     * 日期格式yyyyMMdd
     */
    public static final DateTimeFormatter SHORT_DATE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 日期格式yyyy-MM-dd
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 日期格式yyyyMMddHHmmss
     */
    public static final DateTimeFormatter SHORT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    /**
     * 日期格式yyyy-MM-dd HH:mm:ss
     */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 日期格式yyyy-MM-dd HH:mm:ss SSS
     */
    public static final DateTimeFormatter LONG_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS");

}
