package top.kdla.framework.validator;

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
}
