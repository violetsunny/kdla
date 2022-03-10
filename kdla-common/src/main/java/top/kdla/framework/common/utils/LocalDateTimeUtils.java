/**
 * kanglele Inc. Copyright (c) 2022 All Rights Reserved.
 */
package top.kdla.framework.common.utils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

import top.kdla.framework.common.constants.DateTimeFormat;

/**
 * LocalDateTime --> converter
 * 
 * @author kanglele
 * @version $Id: LocalDateTimeUtils, v 0.1 2022/1/11 19:09 Exp $
 */
public class LocalDateTimeUtils {

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
     * 月初
     * 
     * @param localDateTime
     * @return
     */
    public static LocalDateTime firstDayOfMonth(LocalDateTime localDateTime) {
        return LocalDateTime.of(localDateTime.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()), LocalTime.MIN);
    }

    /**
     * 月末
     * 
     * @param localDateTime
     * @return
     */
    public static LocalDateTime lastDayOfMonth(LocalDateTime localDateTime) {
        return LocalDateTime.of(localDateTime.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX);
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
     * 当前时间到月底有多少秒
     * 
     * @param localDateTime
     * @return
     */
    public static Long secondByLastDayOfMonth(LocalDateTime localDateTime) {
        return Duration
            .between(
                localDateTime,
                LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MAX).with(TemporalAdjusters.lastDayOfMonth()))
            .getSeconds();
    }

    /**
     * 当前时间到月底有多少天
     *
     * @param localDateTime
     * @return
     */
    public static Long daysByLastDayOfMonth(LocalDateTime localDateTime) {
        return Duration
            .between(
                localDateTime,
                LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MAX).with(TemporalAdjusters.lastDayOfMonth()))
            .toDays();
    }
}
