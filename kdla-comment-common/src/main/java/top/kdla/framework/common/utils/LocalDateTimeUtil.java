/**
 * kanglele Inc. Copyright (c) 2022 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.date.TemporalUtil;
import cn.hutool.core.date.Week;
import top.kdla.framework.common.constants.DateTimeFormat;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * LocalDateTime --> converter
 *
 * @author kanglele
 * @version $Id: LocalDateTimeUtils, v 0.1 2022/1/11 19:09 Exp $
 */
public class LocalDateTimeUtil {

    /**
     * LocalDate --> Date
     *
     * @param localDate
     * @return
     */
    public static Date asDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDateTime --> Date
     *
     * @param localDateTime
     * @return
     */
    public static Date asDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Date --> LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDate asLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Date --> LocalDateTime
     *
     * @param date
     * @return
     */
    public static LocalDateTime asLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * str parse(yyyy-MM-dd) --> LocalDate
     *
     * @param str
     * @return
     */
    public static LocalDate asLocalDate(String str) {
        return LocalDate.parse(str, DateTimeFormat.DATE_FORMATTER);
    }

    /**
     * str parse(yyyy-MM-dd HH:mm:ss) --> LocalDateTime
     *
     * @param str
     * @return
     */
    public static LocalDateTime asLocalDateTime(String str) {
        return LocalDateTime.parse(str, DateTimeFormat.DATETIME_FORMATTER);
    }

    /**
     * LocalDate toString(yyyy-MM-dd)
     *
     * @param localDate
     * @return
     */
    public static String toString(LocalDate localDate) {
        return localDate.format(DateTimeFormat.DATE_FORMATTER);
    }

    /**
     * LocalDateTime toString(yyyy-MM-dd HH:mm:ss)
     *
     * @param localDateTime
     * @return
     */
    public static String toString(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormat.DATETIME_FORMATTER);
    }

    /**
     * 修改为一天的开始时间，例如：2020-02-02 00:00:00,000
     *
     * @param time 日期时间
     * @return 一天的开始时间
     */
    public static LocalDateTime beginOfDay(LocalDateTime time) {
        return time.with(LocalTime.MIN);
    }

    /**
     * 修改为一天的结束时间，例如：2020-02-02 23:59:59,999
     *
     * @param time 日期时间
     * @return 一天的结束时间
     */
    public static LocalDateTime endOfDay(LocalDateTime time) {
        return time.with(LocalTime.MAX);
    }

    /**
     * 月初
     *
     * @param localDateTime
     * @return
     */
    public static LocalDateTime firstDayOfMonth(LocalDateTime localDateTime) {
        return beginOfDay(localDateTime).with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * 月末
     *
     * @param localDateTime
     * @return
     */
    public static LocalDateTime lastDayOfMonth(LocalDateTime localDateTime) {
        return endOfDay(localDateTime).with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * 年初
     *
     * @param localDateTime
     * @return
     */
    public static LocalDateTime firstDayOfYear(LocalDateTime localDateTime) {
        return beginOfDay(localDateTime).with(TemporalAdjusters.firstDayOfYear());
    }

    /**
     * 年末
     *
     * @param localDateTime
     * @return
     */
    public static LocalDateTime lastDayOfYear(LocalDateTime localDateTime) {
        return endOfDay(localDateTime).with(TemporalAdjusters.lastDayOfYear());
    }

    /**
     * 是否在这个月
     *
     * @param date
     * @param now
     * @return
     */
    public static Boolean isMonth(LocalDate date, LocalDate now) {
        return (date.isBefore(now.with(TemporalAdjusters.lastDayOfMonth()))
                && date.isAfter(now.with(TemporalAdjusters.firstDayOfMonth())))
                || date.isEqual(now.with(TemporalAdjusters.lastDayOfMonth()))
                || date.isEqual(now.with(TemporalAdjusters.firstDayOfMonth()));
    }

    /**
     * 是否在这个月
     *
     * @param date
     * @param now
     * @return
     */
    public static Boolean isMonthNew(LocalDate date, LocalDate now) {
        return date.getMonthValue() == now.getMonthValue();
    }

    /**
     * 当前时间到月底有多少秒
     *
     * @param localDateTime
     * @return
     */
    public static Long secondToLastDayOfMonth(LocalDateTime localDateTime) {
        return Duration
                .between(
                        localDateTime,
                        lastDayOfMonth(localDateTime))
                .getSeconds();
    }

    /**
     * 当前时间到月底有多少天
     *
     * @param localDateTime
     * @return
     */
    public static Long daysToLastDayOfMonth(LocalDateTime localDateTime) {
        return Duration
                .between(
                        localDateTime,
                        lastDayOfMonth(localDateTime))
                .toDays();
    }



    /**
     * {@link TemporalAccessor}转换为 时间戳（从1970-01-01T00:00:00Z开始的毫秒数）
     *
     * @param temporalAccessor Date对象
     * @return {@link Instant}对象
     * @see TemporalAccessorUtil#toEpochMilli(TemporalAccessor)
     * @since 5.4.1
     */
    public static Long toEpochMilli(TemporalAccessor temporalAccessor) {
        return TemporalAccessorUtil.toEpochMilli(temporalAccessor);
    }

    /**
     * 是否为周末（周六或周日）
     *
     * @param localDateTime 判定的日期{@link LocalDateTime}
     * @return 是否为周末（周六或周日）
     * @since 5.7.6
     */
    public static boolean isWeekend(LocalDateTime localDateTime) {
        return isWeekend(localDateTime.toLocalDate());
    }

    /**
     * 是否为周末（周六或周日）
     *
     * @param localDate 判定的日期{@link LocalDate}
     * @return 是否为周末（周六或周日）
     * @since 5.7.6
     */
    public static boolean isWeekend(LocalDate localDate) {
        final DayOfWeek dayOfWeek = localDate.getDayOfWeek();
        return DayOfWeek.SATURDAY == dayOfWeek || DayOfWeek.SUNDAY == dayOfWeek;
    }

    /**
     * 获取{@link LocalDate}对应的星期值
     *
     * @param localDate 日期{@link LocalDate}
     * @return {@link Week}
     * @since 5.7.14
     */
    public static Week dayOfWeek(LocalDate localDate) {
        return Week.of(localDate.getDayOfWeek());
    }

    /**
     * 相差多少年
     * @param startTime
     * @param endTime
     * @return
     */
    public static Long betweenYear(LocalDateTime startTime, LocalDateTime endTime) {
        return TemporalUtil.between(startTime, endTime, ChronoUnit.YEARS);
    }

}
