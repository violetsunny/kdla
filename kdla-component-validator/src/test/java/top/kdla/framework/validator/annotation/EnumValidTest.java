package top.kdla.framework.validator.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：EnumValid此前两处缺陷：
 * 1) value==null返回FALSE，违反JSR-303"null放行交给@NotNull"约定，可选字段恒不通过；
 * 2) getMethod精确匹配要求参数类型与value运行时类型完全一致，
 *    校验方法参数声明为父类型（CharSequence/Object）时永远找不到，误判FALSE。
 */
class EnumValidTest {

    enum Color {
        RED, GREEN;

        public static boolean checkString(String v) {
            return "RED".equals(v);
        }

        public static boolean checkCharSequence(CharSequence v) {
            return "RED".contentEquals(v);
        }

        public static boolean checkNoArg() {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private EnumValid.Validator newValidator(String checkMethod) {
        EnumValid ann = mock(EnumValid.class);
        when(ann.checkMethod()).thenReturn(checkMethod);
        when(ann.enumClass()).thenReturn((Class) Color.class);
        EnumValid.Validator validator = new EnumValid.Validator();
        validator.initialize(ann);
        return validator;
    }

    @Test
    void nullValue_returnsTrue_perJsr303() {
        // 修复前：null直接FALSE
        assertTrue(newValidator("checkString").isValid(null, null),
                "null必须放行（JSR-303约定），强制非空场景配合@NotNull");
    }

    @Test
    void parameterAssignableMatch_found() {
        // 修复前：getMethod("checkCharSequence", String.class)精确匹配失败→无参回退也不存在→FALSE
        assertTrue(newValidator("checkCharSequence").isValid("RED", null),
                "参数声明为父类型CharSequence时必须按可赋值性命中");
        assertFalse(newValidator("checkCharSequence").isValid("GREEN", null));
    }

    @Test
    void exactParameterType_stillWorks() {
        assertTrue(newValidator("checkString").isValid("RED", null));
        assertFalse(newValidator("checkString").isValid("GREEN", null));
    }

    @Test
    void noArgMethod_fallback() {
        assertTrue(newValidator("checkNoArg").isValid("anything", null), "无参校验方法回退保持可用");
    }

    @Test
    void methodNotFound_returnsFalse() {
        assertFalse(newValidator("nonExistentMethod").isValid("RED", null));
    }

    @Test
    void blankCheckMethod_returnsFalse() {
        assertFalse(newValidator("").isValid("RED", null));
    }
}
