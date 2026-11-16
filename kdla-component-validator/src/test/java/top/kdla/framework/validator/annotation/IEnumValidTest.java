package top.kdla.framework.validator.annotation;

import org.junit.jupiter.api.Test;
import top.kdla.framework.dto.IEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：IEnumValid此前两处缺陷：
 * 1) checkForNull默认true，null字段恒判非法，违反JSR-303"null放行交给@NotNull"约定；
 * 2) enumConstant.getCode().equals(value)在枚举code为null时抛NPE。
 * 修复后：默认false（null放行）；Objects.equals比较防NPE。
 */
class IEnumValidTest {

    enum TestEnum implements IEnum<String> {
        A("a"),
        NULL_CODE(null);

        private final String code;

        TestEnum(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return name();
        }
    }

    @SuppressWarnings("unchecked")
    private IEnumValid.IEnumValidator newValidator(boolean checkForNull) {
        IEnumValid ann = mock(IEnumValid.class);
        when(ann.value()).thenReturn((Class) TestEnum.class);
        when(ann.checkForNull()).thenReturn(checkForNull);
        IEnumValid.IEnumValidator validator = new IEnumValid.IEnumValidator();
        validator.initialize(ann);
        return validator;
    }

    @Test
    void checkForNull_defaultIsFalse() throws Exception {
        // 修复前：默认true，未显式配置checkForNull=false的null字段恒校验失败
        Object defaultValue = IEnumValid.class.getMethod("checkForNull").getDefaultValue();
        assertEquals(Boolean.FALSE, defaultValue, "checkForNull默认值必须为false");
    }

    @Test
    void nullValue_checkForNullFalse_passes() {
        assertTrue(newValidator(false).isValid(null, null), "checkForNull=false时null必须放行");
    }

    @Test
    void nullValue_checkForNullTrue_fails() {
        assertFalse(newValidator(true).isValid(null, null), "checkForNull=true时null判非法（保留原能力）");
    }

    @Test
    void enumWithNullCode_noNPE() {
        // 修复前：遍历到NULL_CODE时getCode().equals(value)抛NPE
        assertFalse(newValidator(false).isValid("x", null), "无匹配code时返回false而非NPE");
        assertTrue(newValidator(false).isValid("a", null), "正常code匹配保持可用");
    }
}
