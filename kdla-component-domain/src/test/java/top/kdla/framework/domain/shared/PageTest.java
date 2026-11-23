package top.kdla.framework.domain.shared;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：domain的Page此前两处缺陷：
 * 1) sameValueAs恒返回false，违反ValueObject按值比较契约；
 * 2) pageSize=0时(int)Math.ceil(Infinity)溢出为Integer.MAX_VALUE。
 * 修复后：按字段值比较；pageSize<=0时totalPage=0。
 */
class PageTest {

    @Test
    void sameValueAs_equalFields_true() {
        Page<String> a = new Page<>(Arrays.asList("x", "y"), 10, 5, 1);
        Page<String> b = new Page<>(Arrays.asList("x", "y"), 10, 5, 1);

        // 修复前：恒false
        assertTrue(a.sameValueAs(b), "字段值全相等的值对象必须判等");
        assertTrue(a.sameValueAs(a), "自反性");
    }

    @Test
    void sameValueAs_differentFields_false() {
        Page<String> base = new Page<>(Arrays.asList("x"), 10, 5, 1);

        assertFalse(base.sameValueAs(new Page<>(Arrays.asList("x"), 11, 5, 1)), "totalCount不同必须不等");
        assertFalse(base.sameValueAs(new Page<>(Arrays.asList("x"), 10, 4, 1)), "pageSize不同必须不等");
        assertFalse(base.sameValueAs(new Page<>(Arrays.asList("x"), 10, 5, 2)), "currPage不同必须不等");
        assertFalse(base.sameValueAs(new Page<>(Arrays.asList("z"), 10, 5, 1)), "list不同必须不等");
        assertFalse(base.sameValueAs(null), "null必须不等");
    }

    @Test
    void pageSizeZero_totalPageZero_notOverflow() {
        Page<String> page = new Page<>(Collections.emptyList(), 100, 0, 1);

        // 修复前：(int)Math.ceil(100/0.0)=(int)Infinity=Integer.MAX_VALUE
        assertEquals(0, page.getTotalPage(), "pageSize<=0时totalPage必须为0，不得溢出");
    }

    @Test
    void negativePageSize_totalPageZero() {
        Page<String> page = new Page<>(Collections.emptyList(), 100, -5, 1);

        assertEquals(0, page.getTotalPage());
    }

    @Test
    void normalTotalPage_ceiling() {
        assertEquals(3, new Page<>(Collections.emptyList(), 11, 5, 1).getTotalPage(), "11/5向上取整=3");
        assertEquals(2, new Page<>(Collections.emptyList(), 10, 5, 1).getTotalPage(), "整除不加1");
        assertEquals(0, new Page<>(Collections.emptyList(), 0, 5, 1).getTotalPage());
    }
}
