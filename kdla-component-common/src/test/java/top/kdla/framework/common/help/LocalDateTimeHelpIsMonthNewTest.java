package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：isMonthNew 旧实现只比较 getMonthValue()，
 * 2024-01 与 2025-01 会被误判为"同月"。修复后用 YearMonth 同时比较年份。
 */
class LocalDateTimeHelpIsMonthNewTest {

    @Test
    void sameMonthDifferentYear_mustBeFalse() {
        assertFalse(LocalDateTimeHelp.isMonthNew(LocalDate.of(2024, 1, 15), LocalDate.of(2025, 1, 10)),
                "跨年同月份必须判定为不在本月");
    }

    @Test
    void sameYearMonth_mustBeTrue() {
        assertTrue(LocalDateTimeHelp.isMonthNew(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)),
                "同年同月必须判定为在本月");
    }

    @Test
    void sameYearDifferentMonth_mustBeFalse() {
        assertFalse(LocalDateTimeHelp.isMonthNew(LocalDate.of(2025, 3, 31), LocalDate.of(2025, 4, 1)),
                "同年不同月必须判定为不在本月");
    }
}
