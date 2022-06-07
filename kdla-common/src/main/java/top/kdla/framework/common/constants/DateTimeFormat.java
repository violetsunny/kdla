package top.kdla.framework.common.constants;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 时间格式的常量
 * @author kll
 */
public interface DateTimeFormat {

    /**
     * 日期格式yyyy-MM-dd
     */
    String DATE1 = "yyyy-MM-dd";
    /**
     * 日期格式yyyy/MM/dd
     */
    String DATE2 = "yyyy/MM/dd";

    /**
     * 日期格式yyyyMMdd
     */
    String DATE3 = "yyyyMMdd";

    /**
     * 日期格式yyMMdd
     */
    String FORMAT_YYMMDD = "yyMMdd";


    /**
     * 日期小时格式yyMMddHH
     */
    String FORMAT_YYMMDDHH = "yyMMddHH";

    /**
     * 日期时间yyyy-MM-dd HH:mm:ss
     */
    String DATETIME1 = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期时间yyyy/MM/dd HH:mm:ss
     */
    String DATETIME2 = "yyyy/MM/dd HH:mm:ss";
    /**
     * 日期时间yyyy-MM-dd HH:mm:ss.SSS
     */
    String DATETIMEWITHMILLISECOND1 = "yyyy-MM-dd HH:mm:ss.SSS";
    /**
     * 日期时间yyyy/MM/dd HH:mm:ss.SSS
     */
    String DATETIMEWITHMILLISECOND2 = "yyyy/MM/dd HH:mm:ss.SSS";

    /**
     * 日期格式yyyyMMddHHmmssSSS
     */
    String DATETIMESTAMP = "yyyyMMddHHmmssSSS";

    /**
     * es 日期格式
     */
    String ES_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    /**
     * es 日期格式
     */
    DateTimeFormatter ES_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.CHINA);

    /**
     * 日期格式yyyyMMdd
     */
    DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    /**
     * 日期格式yyyyMMdd
     */
    DateTimeFormatter SHORT_DATE_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 日期格式yyyy-MM-dd
     */
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 日期格式yyyyMMddHHmmss
     */
    DateTimeFormatter SHORT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    /**
     * 日期格式yyyy-MM-dd HH:mm:ss
     */
    DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 日期格式yyyy-MM-dd HH:mm:ss SSS
     */
    DateTimeFormatter LONG_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS");

}
