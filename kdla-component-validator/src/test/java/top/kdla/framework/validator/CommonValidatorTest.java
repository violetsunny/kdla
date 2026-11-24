package top.kdla.framework.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 回归测试：CommonValidator此前在实例初始化块中Locale.setDefault(Locale.CHINA)，
 * 每次new都改写JVM全局默认Locale（影响宿主应用所有Locale敏感逻辑：数字/日期格式化、大小写转换等，红线）。
 * 修复后：删除该副作用，消息插值使用JVM默认Locale。
 */
class CommonValidatorTest {

    @Test
    void construction_doesNotChangeJvmDefaultLocale() {
        Locale original = Locale.getDefault();
        Locale probe = Locale.JAPAN;
        Locale.setDefault(probe);
        try {
            // 修复前：构造即强制setDefault(Locale.CHINA)
            new CommonValidator();
            assertEquals(probe, Locale.getDefault(), "构造CommonValidator不得改写JVM全局默认Locale");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void defaultValidate_null_throwsBizException() {
        CommonValidator validator = new CommonValidator();
        assertThrows(BizException.class, () -> validator.defaultValidate(null),
                "null入参必须抛BizException（原有行为保持）");
    }

    @Test
    void construction_isLazy_noFactoryUntilFirstValidate() throws Exception {
        CommonValidator validator = new CommonValidator();
        java.lang.reflect.Field factoryField = CommonValidator.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        // 修复前：实例初始化块急建factory（且无factory字段，反射直接NoSuchFieldException）
        assertNull(factoryField.get(validator), "构造时不得建factory");
        validator.defaultValidate(new Object());
        assertNotNull(factoryField.get(validator), "首次校验才懒加载factory");
    }

    @Test
    void close_thenValidate_reinitializes() {
        CommonValidator validator = new CommonValidator();
        validator.defaultValidate(new Object());
        validator.close();
        assertDoesNotThrow(() -> validator.defaultValidate(new Object()), "close后再校验须重新懒加载");
    }
}
