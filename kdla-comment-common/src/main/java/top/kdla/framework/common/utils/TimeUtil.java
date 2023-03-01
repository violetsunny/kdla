package top.kdla.framework.common.utils;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

/**
 * 时间支撑类
 *
 * @author kll
 * @date 2021/8/2
 */
@Component("timeUtilSupport")
public class TimeUtil {

    public static final int MILLION_SECOND_UNIT = 1000;
    public static final int MINUTE_UNIT = 60;

    /**
     * 获得当前时间的ms
     * @return long
     */
    public long now() {
        return currentTimeMillis();
    }

    /**
     * 获得某时间的几天之前的时间 ms
     * @param timestamp 时间
     * @param days 天数
     * @return long
     */
    public long getTimestampWithFewDaysAgo(final long timestamp, final int days) {
        long fewDaysAgo = timestamp - TimeUnit.DAYS.toMillis(days);
        if (fewDaysAgo <= 0) {
            return 0;
        }
        return fewDaysAgo;
    }
    /**
     * 获得某时间的几天之前的时间 ms
     * @param timestamp 时间
     * @param minutes  分钟数
     * @return long
     */
    public long getTimestampWithFewMinutes(final long timestamp, final int minutes) {
        long fewMinutesAgo = timestamp - TimeUnit.MINUTES.toMillis(minutes);
        if (fewMinutesAgo <= 0) {
            return 0;
        }
        return fewMinutesAgo;
    }

    /**
     * 获得两个时间的间隔分钟数
     * @param begin 开始时间ms
     * @param end  结束时间ms
     * @return long
     */
    public long getMinutesBetween(final Optional<Long> begin,
        final Optional<Long> end) {
        if (!begin.isPresent() || !end.isPresent()) {
            return -1;
        }
        return (end.get() - begin.get()) / MILLION_SECOND_UNIT / MINUTE_UNIT;
    }
}
