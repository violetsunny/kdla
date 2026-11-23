package top.kdla.framework.supplement.fieldvaluefinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import top.kdla.framework.domain.ApplicationContextHelp;
import top.kdla.framework.supplement.fieldvaluefinder.annotation.FieldValueFind;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 清单 H37 回归测试：FieldValueFindHelp 批量注入遇已有值即 break，后续对象全部漏注入。
 *
 * <p>旧实现缺陷：batchQuery 的过滤循环中，KEEP_SELF 策略命中已有值时用 break 跳出整个循环，
 * 导致排在它后面的所有对象都不再参与批量查询，字段永远为 null。
 *
 * <p>修复后：改用 continue，仅跳过当前对象，其余对象正常批量注入。
 *
 * <p>无修复则失败：旧代码第二个对象 name 仍为 null，且查询入参为空列表。
 */
class FieldValueFindHelpBatchTest {

    private NameService nameService;
    private FieldValueFindHelp help;

    @BeforeEach
    void setUp() throws Exception {
        nameService = new NameService();
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(NameService.class)).thenReturn(nameService);
        new ApplicationContextHelp().setApplicationContext(context);
        help = new FieldValueFindHelp();
    }

    @AfterEach
    void clearStaticContext() throws Exception {
        Field field = ApplicationContextHelp.class.getDeclaredField("applicationContext");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void keepSelf_skipsOnlyCurrentObjectAndStillInjectsTheRest() {
        KeepSelfRow first = new KeepSelfRow(1L, "existing");
        KeepSelfRow second = new KeepSelfRow(2L, null);

        help.process(Arrays.asList(first, second));

        assertEquals("existing", first.name, "KEEP_SELF须保留自身已有值");
        assertEquals("name-2", second.name, "已有值对象之后的对象仍须被注入");
        assertEquals(1, nameService.calls.size(), "批量查询只执行一次");
        assertEquals(List.of(2L), nameService.calls.get(0), "已有值的id不应进入查询入参");
    }

    @Test
    void keepSelf_allNullValues_batchQueriedOnce() {
        KeepSelfRow first = new KeepSelfRow(1L, null);
        KeepSelfRow second = new KeepSelfRow(2L, null);

        help.process(Arrays.asList(first, second));

        assertEquals("name-1", first.name);
        assertEquals("name-2", second.name);
        assertEquals(1, nameService.calls.size());
        assertEquals(2, nameService.calls.get(0).size());
    }

    @Test
    void useFrom_overwritesExistingValue() {
        UseFromRow first = new UseFromRow(1L, "stale");
        UseFromRow second = new UseFromRow(2L, null);

        help.process(Arrays.asList(first, second));

        assertEquals("name-1", first.name, "USE_FROM须用查询值覆盖已有值");
        assertEquals("name-2", second.name);
        assertEquals(1, nameService.calls.size());
        assertEquals(2, nameService.calls.get(0).size());
    }

    @Test
    void emptyList_noQueryExecuted() {
        help.process(new ArrayList<>());
        assertEquals(0, nameService.calls.size());
    }

    public static class NameService {
        final List<List<Long>> calls = new ArrayList<>();

        public Map<Long, String> listMapByIds(List<Long> ids) {
            calls.add(new ArrayList<>(ids));
            Map<Long, String> result = new HashMap<>();
            for (Long id : ids) {
                result.put(id, "name-" + id);
            }
            return result;
        }
    }

    public static class KeepSelfRow {
        private final Long id;
        @FieldValueFind(queryClass = NameService.class, queryMethod = "listMapByIds", fromField = "id",
                existPolicy = FieldValueFindHelp.ExistPolicy.KEEP_SELF,
                queryPolicy = FieldValueFindHelp.QueryPolicy.BATCH)
        private String name;

        KeepSelfRow(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class UseFromRow {
        private final Long id;
        @FieldValueFind(queryClass = NameService.class, queryMethod = "listMapByIds", fromField = "id",
                existPolicy = FieldValueFindHelp.ExistPolicy.USE_FROM,
                queryPolicy = FieldValueFindHelp.QueryPolicy.BATCH)
        private String name;

        UseFromRow(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
