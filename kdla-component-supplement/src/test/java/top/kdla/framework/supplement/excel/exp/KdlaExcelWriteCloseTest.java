package top.kdla.framework.supplement.excel.exp;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.kdla.framework.supplement.excel.BaseExcel;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单C2 回归测试（移植自1.x，适配2.x重命名KdlaExcelWriteHelp与静态Collection签名）：
 * writeSheet关闭ExcelWriter，文件被完整flush落盘。
 *
 * <p>旧实现writer创建后未调用finish()，缓冲区未刷盘、文件句柄/输出流泄漏，
 * 生成的xlsx为空或不完整，读取方拿不到数据。
 *
 * <p>无修复则失败：旧代码文件长度为0或读取抛异常，下方断言不通过。
 */
class KdlaExcelWriteCloseTest {

    @TempDir
    Path tempDir;

    public static class Row extends BaseExcel {
        @ExcelProperty("name")
        private String name;

        public Row() {
        }

        public Row(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void writeSheet_finishesWriter_fileCompleteAndReadable() {
        File file = tempDir.resolve("test.xlsx").toFile();
        Map<Class<Row>, Collection<Row>> data = new LinkedHashMap<>();
        data.put(Row.class, Arrays.asList(new Row("a"), new Row("b")));

        KdlaExcelWriteHelp.writeSheet(file, data);

        assertTrue(file.exists(), "导出文件应存在");
        assertTrue(file.length() > 0, "导出文件不应为空（旧实现未finish导致空文件）");
        List<Row> read = EasyExcel.read(file).head(Row.class).sheet().doReadSync();
        assertEquals(2, read.size());
        assertEquals("a", read.get(0).getName());
        assertEquals("b", read.get(1).getName());
    }
}