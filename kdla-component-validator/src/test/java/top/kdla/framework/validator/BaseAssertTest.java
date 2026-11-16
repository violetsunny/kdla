package top.kdla.framework.validator;

import org.junit.jupiter.api.Test;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：BaseAssert此前isBlank/isNull语义反转（名为is实为assertNot），
 * 且isBlank只判null/""，纯空白串"   "漏过。
 * 修复后：重命名为notBlank（trim后判空）；isNull与既有notNull(Object,String,String)签名语义完全重复，直接删除。
 * 注意：此为breaking change，全仓已确认无旧方法调用点。
 */
class BaseAssertTest {

    @Test
    void notBlank_whitespaceString_throws() {
        // 修复前：isBlank("   ")只判null/""，纯空白串直接放过
        assertThrows(BizException.class, () -> BaseAssert.notBlank("   ", "CODE", "必须非空白"));
        assertThrows(BizException.class, () -> BaseAssert.notBlank("", "必须非空白"));
        assertThrows(BizException.class, () -> BaseAssert.notBlank(null, "必须非空白"));
        assertThrows(BizException.class, () -> BaseAssert.notBlank(" \t\n ", "CODE", "必须非空白"));
    }

    @Test
    void notBlank_normalString_passes() {
        assertDoesNotThrow(() -> BaseAssert.notBlank("a", "CODE", "必须非空白"));
        assertDoesNotThrow(() -> BaseAssert.notBlank(" a ", "必须非空白"));
    }

    @Test
    void notBlank_errorCodeIOverload_throws() {
        assertThrows(BizException.class, () -> BaseAssert.notBlank("  ", ErrorCode.PARAMETER_ERROR, "必须非空白"));
        assertDoesNotThrow(() -> BaseAssert.notBlank("a", ErrorCode.PARAMETER_ERROR, "必须非空白"));
    }

    @Test
    void notNull_coversRemovedIsNullSemantics() {
        // isNull(Object,String,String)与notNull(Object,String,String)实现完全相同，已删除，语义由notNull承接
        assertThrows(BizException.class, () -> BaseAssert.notNull(null, "CODE", "必须非null"));
        assertDoesNotThrow(() -> BaseAssert.notNull(new Object(), "CODE", "必须非null"));
    }

    @Test
    void oldReversedNameMethods_removed() {
        // breaking change锁定：旧的反转命名方法不得再存在
        assertTrue(Arrays.stream(BaseAssert.class.getDeclaredMethods())
                        .noneMatch(m -> m.getName().equals("isBlank") || m.getName().equals("isNull")),
                "isBlank/isNull已重命名/删除，调用方需迁移到notBlank/notNull");
    }
}
