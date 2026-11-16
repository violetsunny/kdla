package top.kdla.framework.infra.dal.es.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 回归测试：EsSourceData此前用@Builder，builder()不包含父类EsBaseData的id/index字段，
 * 链式构造ES文档时无法设置_id与索引名。修复后：父子类改@SuperBuilder，父类字段可经builder设置。
 */
class EsSourceDataSuperBuilderTest {

    @Test
    void superBuilder_setsParentAndChildFields() {
        EsSourceData data = EsSourceData.builder()
                // 修复前：builder()上不存在id/index方法（父类字段缺失），本行无法编译
                .id("doc-1")
                .index("test-index")
                .type("tweet")
                .source("payload")
                .timestamp(123L)
                .build();

        assertEquals("doc-1", data.getId(), "父类id字段必须可经builder设置");
        assertEquals("test-index", data.getIndex(), "父类index字段必须可经builder设置");
        assertEquals("tweet", data.getType());
        assertEquals("payload", data.getSource());
        assertEquals(123L, data.getTimestamp());
    }

    @Test
    void existingConstructors_stillWork() {
        // 原有构造入口保持不变，@SuperBuilder不得破坏兼容
        EsSourceData byOf = EsSourceData.of(1L, "tweet", "payload");
        assertEquals(1L, byOf.getTimestamp());

        EsSourceData byTwoArgs = new EsSourceData("tweet", "payload");
        assertEquals("tweet", byTwoArgs.getType());
        assertNotNull(byTwoArgs);

        EsSourceData byNoArgs = new EsSourceData();
        assertEquals(0L, byNoArgs.getTimestamp());
    }
}
