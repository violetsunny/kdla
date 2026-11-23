package top.kdla.framework.infra.dal.mybatis.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import top.kdla.framework.infra.dal.mybatis.common.DoConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：PlusPageQuery此前对基本类型long做Objects.nonNull判断（自动装箱后恒true），
 * 未设置分页参数(0)时Page(0,0)直接透传；getPage(Map)对null params直接put抛NPE。
 * 修复后：改判>0，未设置/负数回退默认值；params判空防护。
 */
class PlusPageQueryTest {

    @Test
    void unsetPaging_fallbackToDefault() {
        PlusPageQuery<Object> query = new PlusPageQuery<>();

        IPage<Object> page = query.getPage();

        // 修复前：Objects.nonNull(0L装箱)恒true → Page(0,0)
        assertEquals(1, page.getCurrent(), "未设置页码必须回退默认1");
        assertEquals(10, page.getSize(), "未设置页大小必须回退默认10");
    }

    @Test
    void negativePaging_fallbackToDefault() {
        PlusPageQuery<Object> query = new PlusPageQuery<>(-1, -10, null, null);

        IPage<Object> page = query.getPage();

        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
    }

    @Test
    void positivePaging_used() {
        PlusPageQuery<Object> query = new PlusPageQuery<>(2, 20, null, null);

        IPage<Object> page = query.getPage();

        assertEquals(2, page.getCurrent());
        assertEquals(20, page.getSize());
    }

    @Test
    void getPageWithNullParams_noNPE() {
        PlusPageQuery<Object> query = new PlusPageQuery<>(1, 10, null, null);

        // 修复前：params.put直接NPE
        assertDoesNotThrow(() -> query.getPage((Map<String, Object>) null));
    }

    @Test
    void getPageWithParams_putsPageEntry() {
        PlusPageQuery<Object> query = new PlusPageQuery<>(1, 10, null, null);
        Map<String, Object> params = new HashMap<>();

        query.getPage(params);

        assertTrue(params.get(DoConstants.PAGE) instanceof IPage, "非null params必须放入分页对象");
    }
}
