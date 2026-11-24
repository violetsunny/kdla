package top.kdla.framework.supplement.fieldvaluefinder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import top.kdla.framework.domain.ApplicationContextHelp;
import top.kdla.framework.supplement.fieldvaluefinder.annotation.FieldValueFind;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 安全审计 C-11 回归测试：FieldValueFindHelp 失败日志整体序列化 sourceObject，业务字段值入日志。
 *
 * <p>修复后：日志只输出源类名 + 字段名 + 查询目标（queryClass#queryMethod），不序列化对象值。
 *
 * <p>无修复则失败：旧代码 JacksonUtil.toJson(dto) 把手机号明文写进 ERROR 日志。
 */
class FieldValueFindHelpLogSanitizeTest {

    private static final String SECRET = "13800138000";

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(ThrowingService.class)).thenReturn(new ThrowingService());
        new ApplicationContextHelp().setApplicationContext(context);
        logger = (Logger) LoggerFactory.getLogger(FieldValueFindHelp.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() throws Exception {
        logger.detachAppender(appender);
        Field field = ApplicationContextHelp.class.getDeclaredField("applicationContext");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void directQueryFailure_logOmitsSourceObjectValues() {
        DirectRow row = new DirectRow(1L, SECRET);
        assertThrows(RuntimeException.class, () -> new FieldValueFindHelp().process(List.of(row)));
        assertSanitized();
    }

    @Test
    void batchQueryFailure_logOmitsSourceObjectValues() {
        BatchRow row = new BatchRow(1L, SECRET);
        assertThrows(RuntimeException.class, () -> new FieldValueFindHelp().process(List.of(row)));
        assertSanitized();
    }

    private void assertSanitized() {
        List<ILoggingEvent> errors = appender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .toList();
        assertFalse(errors.isEmpty(), "失败须记录 ERROR 日志");
        for (ILoggingEvent event : errors) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(SECRET), "日志不得含业务对象字段值，实际:" + msg);
            assertTrue(msg.contains("ThrowingService"), "须保留查询目标便于定位，实际:" + msg);
        }
    }

    public static class ThrowingService {
        public String findOne(Long id) {
            throw new IllegalStateException("query failed");
        }

        public Map<Long, String> listMapByIds(List<Long> ids) {
            throw new IllegalStateException("batch query failed");
        }
    }

    public static class DirectRow {
        private final Long id;
        private final String mobile;
        @FieldValueFind(queryClass = ThrowingService.class, queryMethod = "findOne", fromField = "id",
                queryPolicy = FieldValueFindHelp.QueryPolicy.DIRECT)
        private String name;

        DirectRow(Long id, String mobile) {
            this.id = id;
            this.mobile = mobile;
        }
    }

    public static class BatchRow {
        private final Long id;
        private final String mobile;
        @FieldValueFind(queryClass = ThrowingService.class, queryMethod = "listMapByIds", fromField = "id",
                queryPolicy = FieldValueFindHelp.QueryPolicy.BATCH)
        private String name;

        BatchRow(Long id, String mobile) {
            this.id = id;
            this.mobile = mobile;
        }
    }
}
