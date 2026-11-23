package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：filter(BigInteger) 曾误用 val.toString(20)（20进制字符串）构造 BigDecimal，
 * 导致 1000 被序列化为 "2a0" 后抛 NumberFormatException 或得到错误数值。
 * 修复后应直接以 BigInteger 原值构造。
 */
class BigDecimalHelpFilterTest {

    @Test
    void filter_bigInteger_keepsOriginalValue() {
        BigDecimal result = BigDecimalHelp.filter(BigInteger.valueOf(1000));
        assertEquals(0, result.compareTo(new BigDecimal("1000")),
                "filter(BigInteger.valueOf(1000)) 必须等于 1000，而不是 20 进制串 \"2a0\" 的解析结果");
    }

    @Test
    void filter_bigIntegerNull_returnsZero() {
        BigDecimal result = BigDecimalHelp.filter((BigInteger) null);
        assertEquals(0, result.compareTo(BigDecimal.ZERO), "null 入参必须回退为 0");
    }
}
