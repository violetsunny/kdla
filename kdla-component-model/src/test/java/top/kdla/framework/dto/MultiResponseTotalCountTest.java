package top.kdla.framework.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：buildSuccess(items, totalCount) 曾静默丢弃 totalCount 参数
 * （类中无对应字段），下游依赖该重载传分页总数时拿到缺数据的响应且无报错。
 * 修复后新增 totalCount 字段并回填。
 */
class MultiResponseTotalCountTest {

    @Test
    void buildSuccess_withExplicitTotalCount_carriesItThrough() {
        MultiResponse<String> resp = MultiResponse.buildSuccess(Arrays.asList("a", "b"), 100);
        assertEquals(100, resp.getTotalCount(), "显式传入的 totalCount 必须回填而非静默丢弃");
        assertEquals(2, resp.getData().size());
    }

    @Test
    void buildSuccess_withoutTotalCount_defaultsToItemsSize() {
        MultiResponse<String> resp = MultiResponse.buildSuccess(Arrays.asList("a", "b", "c"));
        assertEquals(3, resp.getTotalCount());
    }
}
