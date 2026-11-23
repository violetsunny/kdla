package top.kdla.framework.supplement.excel.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * 清单 C7 回归测试：CsvUtil.parseCsv 无限流。
 *
 * <p>旧实现用 Stream.iterate(0, i -> i + 1).map(record::get) 逐列取值，
 * 无限流没有按 record.size() 截断，collect 永不终止（挂死/OOM）。
 *
 * <p>修复后改为 IntStream.range(0, record.size())，按实际列数取值。
 *
 * <p>无修复则失败：旧代码在 5 秒内无法返回，assertTimeoutPreemptively 直接判失败。
 */
class CsvUtilParseTest {

    @Test
    void parseCsv_commaDelimiter_returnsAllRowsAndColumns() {
        List<List<String>> rows = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> CsvUtil.parseCsv("a,b\n1,2", ','));
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), rows);
    }

    @Test
    void parseCsv_customDelimiter_respected() {
        List<List<String>> rows = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> CsvUtil.parseCsv("a;b\n1;2", ';'));
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), rows);
    }
}