package top.kdla.framework.supplement.excel.imp;

import org.junit.jupiter.api.Test;
import top.kdla.framework.supplement.excel.BaseExcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全审计 C-03 回归测试：KdlaExcelReadListener 无行数上限，超大导入无界累积致 OOM。
 *
 * <p>修复后：默认上限 10000（对齐同包 ExcelReadListener），超限显式抛异常；
 * 提供 importRowLimit 覆盖入口。
 *
 * <p>无修复则失败：旧代码第 10001 行照常入列，不抛异常。
 */
class KdlaExcelReadListenerRowLimitTest {

    @Test
    void defaultLimit_is10000() {
        assertEquals(10000, new TestListener().getMaxRow());
    }

    @Test
    void defaultLimit_rejectsRowBeyondLimit() {
        TestListener listener = new TestListener();
        for (int i = 0; i < 10000; i++) {
            listener.invoke(new Row(), null);
        }
        RuntimeException e = assertThrows(RuntimeException.class, () -> listener.invoke(new Row(), null));
        assertTrue(e.getMessage().contains("10000"), "异常须带上限值，实际:" + e.getMessage());
        assertEquals(10000, listener.getDataList().size(), "超限后不得继续累积");
    }

    @Test
    void importRowLimit_overridesDefault() {
        TestListener listener = new TestListener();
        listener.importRowLimit(2);
        listener.invoke(new Row(), null);
        listener.invoke(new Row(), null);
        RuntimeException e = assertThrows(RuntimeException.class, () -> listener.invoke(new Row(), null));
        assertTrue(e.getMessage().contains("2"), "异常须带覆盖后的上限，实际:" + e.getMessage());
        assertEquals(2, listener.getDataList().size());
    }

    @Test
    void withinLimit_allRowsCollected() {
        TestListener listener = new TestListener();
        listener.importRowLimit(3);
        for (int i = 0; i < 3; i++) {
            listener.invoke(new Row(), null);
        }
        assertEquals(3, listener.getDataList().size());
        assertEquals(3, listener.getResList().size());
    }

    private static class Row extends BaseExcel {
    }

    private static class TestListener extends KdlaExcelReadListener<Row, Row> {
        TestListener() {
            super(list -> true);
        }

        @Override
        public Row checkData(Row row) {
            return row;
        }
    }
}
