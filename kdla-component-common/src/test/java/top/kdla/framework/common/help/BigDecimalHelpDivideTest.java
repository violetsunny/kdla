package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：divide 旧实现用 BigDecimal.ZERO.equals(...) 判零，equals 会比较 scale，
 * 0.00 不等于 ZERO，导致除数为 0.00 时漏判、抛 ArithmeticException（Division by zero）。
 * 修复后改用 compareTo == 0 判零。
 */
class BigDecimalHelpDivideTest {

    @Test
    void divide_divisorZeroWithScale_returnsZeroNotThrow() {
        BigDecimal result = BigDecimalHelp.divide(BigDecimal.ONE, new BigDecimal("0.00"));
        assertEquals(0, result.compareTo(BigDecimal.ZERO),
                "除数为 0.00（带 scale 的零）必须判零返回 ZERO，不能抛 ArithmeticException");
    }

    @Test
    void divide_dividendZeroWithScale_returnsZero() {
        BigDecimal result = BigDecimalHelp.divide(new BigDecimal("0.000"), BigDecimal.ONE);
        assertEquals(0, result.compareTo(BigDecimal.ZERO), "被除数为 0.000 必须判零返回 ZERO");
    }

    @Test
    void divide_normalCase_stillWorks() {
        BigDecimal result = BigDecimalHelp.divide(BigDecimal.ONE, new BigDecimal("2"));
        assertEquals(0, result.compareTo(new BigDecimal("0.5")), "正常除法结果不受影响");
    }
}
