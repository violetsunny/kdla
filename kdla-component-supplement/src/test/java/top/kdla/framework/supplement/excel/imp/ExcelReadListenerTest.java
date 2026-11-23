package top.kdla.framework.supplement.excel.imp;

import com.alibaba.excel.context.AnalysisContext;
import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 清单 H42 回归测试：ExcelReadListener 空单元格 NPE、缺失 recordHandler 静默跳过、
 * 解析异常仍报 success、空批次合并 NPE。
 *
 * <p>旧实现四处缺陷：
 * (1) doAfterAllAnalysed 直接 getTempCellData().getColumnIndex()，空 sheet 时 NPE；
 * (2) recordFunction 为 null 时静默 return，整批跳过且不回调结果；
 * (3) 失败判定只看 errors/importError，onException 收集的 errorMessageList 被忽略，解析异常仍报 success；
 * (4) merge 无错误批次（errors=null）时 NPE。
 *
 * <p>无修复则失败：用例1旧代码抛 NPE 而非 BizException；用例2旧代码 success=true；
 * 用例3旧代码 merge 空批次 NPE。
 */
class ExcelReadListenerTest {

    public static class Row {
        private final String name;

        public Row(String name) {
            this.name = name;
        }
    }

    private AnalysisContext mockContext() {
        AnalysisContext ctx = mock(AnalysisContext.class, RETURNS_DEEP_STUBS);
        when(ctx.readRowHolder().getRowIndex()).thenReturn(0);
        when(ctx.readSheetHolder().getTempCellData()).thenReturn(null);
        return ctx;
    }

    private ExcelRowConverter<Row> stringConverter() {
        return new ExcelRowConverter<Row>() {
            @SuppressWarnings("unchecked")
            @Override
            public <R> R convert(int rowIndex, Row data) {
                return (R) data.name;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <E> E errorConvert(int rowIndex, Row data, String errorMessage) {
                return (E) ("row" + rowIndex + ":" + errorMessage);
            }
        };
    }

    @Test
    void doAfterAllAnalysed_missingRecordHandler_throwsBizException() {
        ExcelReadListener<Row, String> listener = new ExcelReadListener<>();
        AnalysisContext ctx = mockContext();
        BizException ex = assertThrows(BizException.class, () -> listener.doAfterAllAnalysed(ctx));
        assertTrue(ex.getMessage().contains("recordHandler"));
    }

    @Test
    void onException_withoutErrorConverter_resultMarkedFailed() throws Exception {
        ExcelReadListener<Row, String> listener = new ExcelReadListener<>();
        AtomicReference<ExcelImportResult> resultRef = new AtomicReference<>();
        listener.recordHandler(batch -> new ExcelImportError())
                .resultHandler(resultRef::set);
        AnalysisContext ctx = mockContext();
        listener.onException(new RuntimeException("bad cell"), ctx);
        listener.doAfterAllAnalysed(ctx);
        ExcelImportResult result = resultRef.get();
        assertFalse(result.isSuccess());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getErrorMessage().stream().anyMatch(m -> m.contains("导入异常")));
    }

    @Test
    void mixedBatches_emptyBatchMergedWithoutNpe_errorCountCorrect() {
        ExcelReadListener<Row, String> listener = new ExcelReadListener<>();
        AtomicReference<ExcelImportResult> resultRef = new AtomicReference<>();
        listener.converter(stringConverter())
                .processCount(1)
                .recordHandler(batch -> {
                    ExcelImportError<String> error = new ExcelImportError<>();
                    if (batch.contains("bad")) {
                        error.addError("bad-row");
                    }
                    return error;
                })
                .resultHandler(resultRef::set);
        AnalysisContext ctx = mockContext();
        listener.invoke(new Row("good"), ctx);
        listener.invoke(new Row("bad"), ctx);
        listener.doAfterAllAnalysed(ctx);
        ExcelImportResult result = resultRef.get();
        assertFalse(result.isSuccess());
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getErrorCount());
    }

    @Test
    void allValidRows_resultSuccess() {
        ExcelReadListener<Row, String> listener = new ExcelReadListener<>();
        AtomicReference<ExcelImportResult> resultRef = new AtomicReference<>();
        listener.converter(stringConverter())
                .recordHandler(batch -> new ExcelImportError())
                .resultHandler(resultRef::set);
        AnalysisContext ctx = mockContext();
        listener.invoke(new Row("good"), ctx);
        listener.doAfterAllAnalysed(ctx);
        ExcelImportResult result = resultRef.get();
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalCount());
    }
}