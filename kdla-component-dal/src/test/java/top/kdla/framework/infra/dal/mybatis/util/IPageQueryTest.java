package top.kdla.framework.infra.dal.mybatis.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import top.kdla.framework.infra.dal.mybatis.common.DoConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：IPageQuery.mapPage此前对params值做(String)强转+Long.parseLong无防护，
 * 值为Integer等非String类型抛ClassCastException，非数字字符串抛NumberFormatException；
 * 且limit无上限、负数分页参数直接透传。
 * 修复后：String.valueOf+try-parse失败回退默认值，负数归默认，limit上限500。
 */
class IPageQueryTest {

    private final IPageQuery<Object> query = new IPageQuery<>();

    @Test
    void nonStringValue_noClassCastException() {
        Map<String, Object> params = new HashMap<>();
        // 修复前：(String)强转Integer直接ClassCastException
        params.put(DoConstants.PAGE, 3);
        params.put(DoConstants.LIMIT, 20);

        IPage<Object> page = query.mapPage(params);

        assertEquals(3, page.getCurrent());
        assertEquals(20, page.getSize());
    }

    @Test
    void unparsableString_fallbackToDefault() {
        Map<String, Object> params = new HashMap<>();
        // 修复前：Long.parseLong("abc")抛NumberFormatException
        params.put(DoConstants.PAGE, "abc");
        params.put(DoConstants.LIMIT, "10x");

        IPage<Object> page = query.mapPage(params);

        assertEquals(1, page.getCurrent(), "解析失败必须回退默认页码1");
        assertEquals(10, page.getSize(), "解析失败必须回退默认页大小10");
    }

    @Test
    void negativeValues_fallbackToDefault() {
        Map<String, Object> params = new HashMap<>();
        params.put(DoConstants.PAGE, "-2");
        params.put(DoConstants.LIMIT, "-5");

        IPage<Object> page = query.mapPage(params);

        assertEquals(1, page.getCurrent(), "负数页码必须归默认");
        assertEquals(10, page.getSize(), "负数页大小必须归默认");
    }

    @Test
    void limitOverCap_cappedTo500() {
        Map<String, Object> params = new HashMap<>();
        params.put(DoConstants.LIMIT, "100000");

        IPage<Object> page = query.mapPage(params);

        assertEquals(500, page.getSize(), "limit必须封顶500，防止超大分页拖垮数据库");
    }

    @Test
    void absentValues_useDefaults() {
        IPage<Object> page = query.mapPage(new HashMap<>());

        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
    }

    @Test
    void zeroValues_fallbackToDefault() {
        Map<String, Object> params = new HashMap<>();
        params.put(DoConstants.PAGE, 0);
        params.put(DoConstants.LIMIT, 0);

        IPage<Object> page = query.mapPage(params);

        // 修复前：limit=0透传→空页，与PlusPageQuery（0回退10）不一致
        assertEquals(1, page.getCurrent(), "页码0必须回退默认");
        assertEquals(10, page.getSize(), "页大小0必须回退默认");
    }

    @Test
    void nonStringOrderValues_noClassCastException() {
        Map<String, Object> params = new HashMap<>();
        // 修复前：(String)强转Map/List抛ClassCastException 500
        params.put(DoConstants.ORDER_FIELD, Map.of("col", "name"));
        params.put(DoConstants.ORDER, List.of("asc"));

        IPage<Object> page = assertDoesNotThrow(() -> query.mapPage(params));
        assertTrue(page.orders().isEmpty(), "非字符串排序参数须按缺省处理");
    }

    @Test
    void nullParams_noNPE() {
        // 修复前：params.get直接NPE
        IPage<Object> page = assertDoesNotThrow(() -> query.mapPage(null));
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
    }
}
