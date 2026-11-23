package top.kdla.framework.supplement.excel.imp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 H42 回归测试：ExcelImportError.merge 空批次 NPE。
 *
 * <p>旧实现 merge 直接 error.getErrors().forEach(...)，
 * 无错误批次的 errors 为 null（recordHandler 返回 new ExcelImportError()），合并时 NPE。
 *
 * <p>修复后：error 为 null 或 errors 为 null 时直接跳过。
 *
 * <p>无修复则失败：旧代码 merge(null) 与 merge(空批次) 均抛 NullPointerException。
 */
class ExcelImportErrorMergeTest {

    @Test
    void merge_nullArgument_noNpe() {
        ExcelImportError<String> target = new ExcelImportError<>();
        assertDoesNotThrow(() -> target.merge(null));
        assertTrue(target.getErrors() == null || target.getErrors().isEmpty());
    }

    @Test
    void merge_batchWithoutErrors_noNpe() {
        ExcelImportError<String> target = new ExcelImportError<>();
        ExcelImportError<String> emptyBatch = new ExcelImportError<>();
        assertDoesNotThrow(() -> target.merge(emptyBatch));
        assertTrue(target.getErrors() == null || target.getErrors().isEmpty());
    }

    @Test
    void merge_normalBatch_errorsAccumulated() {
        ExcelImportError<String> target = new ExcelImportError<>();
        ExcelImportError<String> batch1 = new ExcelImportError<>();
        batch1.addError("e1");
        ExcelImportError<String> batch2 = new ExcelImportError<>();
        batch2.addError("e2");
        target.merge(batch1);
        target.merge(batch2);
        assertEquals(2, target.getErrors().size());
        assertTrue(target.hasError());
    }
}