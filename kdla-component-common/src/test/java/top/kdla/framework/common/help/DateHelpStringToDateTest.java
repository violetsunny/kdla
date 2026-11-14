package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 回归测试：stringToDate 旧实现固定走 joda parseLocalDateTime，
 * 传入纯日期 pattern（如 yyyy-MM-dd）时因缺失时间字段抛异常。
 * 修复后按 pattern 是否含时间字段分派 parseLocalDate / parseLocalDateTime。
 */
class DateHelpStringToDateTest {

    @Test
    void dateOnlyPattern_parsesWithoutException() {
        Date date = DateHelp.stringToDate("2024-05-20", DateHelp.DATE_PATTERN);
        assertNotNull(date, "纯日期 pattern 必须能正常解析，不能抛异常");
        assertEquals("2024-05-20", DateHelp.format(date, DateHelp.DATE_PATTERN));
    }

    @Test
    void dateTimePattern_stillParses() {
        Date date = DateHelp.stringToDate("2024-05-20 10:30:00", DateHelp.DATE_TIME_PATTERN);
        assertNotNull(date);
        assertEquals("2024-05-20 10:30:00", DateHelp.format(date, DateHelp.DATE_TIME_PATTERN),
                "含时间字段的 pattern 解析行为不受影响");
    }

    @Test
    void slashDateOnlyPattern_parsesWithoutException() {
        Date date = DateHelp.stringToDate("2024/05/20", DateHelp.DATE_PATTERN_1);
        assertNotNull(date);
        assertEquals("2024/05/20", DateHelp.format(date, DateHelp.DATE_PATTERN_1));
    }

    @Test
    void blankInput_returnsNull() {
        assertNull(DateHelp.stringToDate("", DateHelp.DATE_PATTERN), "空串入参保持返回 null 的原语义");
    }
}
