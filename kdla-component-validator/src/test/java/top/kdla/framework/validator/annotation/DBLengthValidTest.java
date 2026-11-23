package top.kdla.framework.validator.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：DBLengthValid此前用pow(length).intValue()/longValue()降级比较，
 * length>=10时10^10溢出int、length>=19时10^19溢出long，合法值被误判超限。
 * 修复后：BigDecimal全程比较，不降级int/long。
 */
class DBLengthValidTest {

    private DBLengthValid.Validator newValidator(int length, int decimal) {
        DBLengthValid ann = mock(DBLengthValid.class);
        when(ann.length()).thenReturn(length);
        when(ann.decimal()).thenReturn(decimal);
        DBLengthValid.Validator validator = new DBLengthValid.Validator();
        validator.initialize(ann);
        return validator;
    }

    @Test
    void integerValue_length10_noIntOverflow() {
        // 修复前：10^10.intValue()=1410065408，2000000000>1410065407被误判超限
        assertTrue(newValidator(10, 2).isValid(2_000_000_000, null),
                "2000000000在10位整数范围内必须判合法");
        assertFalse(newValidator(9, 2).isValid(2_000_000_000, null),
                "超出9位上限(999999999)必须判非法");
    }

    @Test
    void longValue_length19_noLongOverflow() {
        // 修复前：10^19.longValue()溢出为负数，Long.MAX_VALUE被误判超限
        assertTrue(newValidator(19, 2).isValid(Long.MAX_VALUE, null),
                "Long.MAX_VALUE在19位范围内必须判合法");
    }

    @Test
    void boundaryValues_correct() {
        assertTrue(newValidator(9, 2).isValid(999_999_999, null), "恰好等于10^9-1判合法");
        assertFalse(newValidator(9, 2).isValid(1_000_000_000, null), "10^9超9位上限判非法");
    }

    @Test
    void stringAndNull_behaviorUnchanged() {
        assertTrue(newValidator(3, 2).isValid("abc", null));
        assertFalse(newValidator(3, 2).isValid("abcd", null));
        assertTrue(newValidator(3, 2).isValid(null, null), "null放行语义保持不变");
    }
}
