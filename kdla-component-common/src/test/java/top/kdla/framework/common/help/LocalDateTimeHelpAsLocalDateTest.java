package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 回归测试：asLocalDate(long)/asLocalDateTime(long) 旧 1.x 语义对非 10/13 位时间戳返回 null，
 * 2.x 曾改抛 IllegalArgumentException 破坏下游判空分支。修复后恢复返回 null。
 */
class LocalDateTimeHelpAsLocalDateTest {

    @Test
    void invalidLength_returnsNull_notThrows() {
        assertNull(LocalDateTimeHelp.asLocalDate(123L), "非 10/13 位必须返回 null 保持下游判空兼容");
        assertNull(LocalDateTimeHelp.asLocalDateTime(123L), "非 10/13 位必须返回 null 保持下游判空兼容");
    }

    @Test
    void tenDigits_epochSecond_parses() {
        assertNotNull(LocalDateTimeHelp.asLocalDate(1700000000L));
        assertNotNull(LocalDateTimeHelp.asLocalDateTime(1700000000L));
    }

    @Test
    void thirteenDigits_epochMilli_parses() {
        assertNotNull(LocalDateTimeHelp.asLocalDate(1700000000000L));
        assertNotNull(LocalDateTimeHelp.asLocalDateTime(1700000000000L));
    }
}