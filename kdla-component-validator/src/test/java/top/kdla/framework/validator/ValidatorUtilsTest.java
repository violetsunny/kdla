package top.kdla.framework.validator;

import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 回归 C-05：ValidatorUtils 静态块类加载即建 factory，下游无实现时 ExceptionInInitializerError。
 * 修复后：懒加载，首次 validate 才建 factory；校验行为不变。
 */
class ValidatorUtilsTest {

    static class Dto {
        @NotNull
        private String name;
    }

    @Test
    void validate_rebuildsValidatorAfterReset() throws Exception {
        Field field = ValidatorUtils.class.getDeclaredField("validator");
        field.setAccessible(true);
        Object original = field.get(null);
        field.set(null, null);
        try {
            Dto dto = new Dto();
            dto.name = "ok";
            // 修复前：无懒加载，validator为null直接NPE；修复后：首次调用重建
            assertDoesNotThrow(() -> ValidatorUtils.validate(dto));
        } finally {
            field.set(null, original);
        }
    }

    @Test
    void validate_invalidObject_throwsBizException() {
        assertThrows(BizException.class, () -> ValidatorUtils.validate(new Dto()));
    }

    @Test
    void validate_validObject_passes() {
        Dto dto = new Dto();
        dto.name = "ok";
        assertDoesNotThrow(() -> ValidatorUtils.validate(dto));
    }
}