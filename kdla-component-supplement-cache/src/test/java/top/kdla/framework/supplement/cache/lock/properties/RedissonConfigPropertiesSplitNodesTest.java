package top.kdla.framework.supplement.cache.lock.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 主题2 回归测试：RedissonConfigProperties.splitNodes 地址解析 fail-fast。
 *
 * <p>旧实现把整个逗号分隔串当单个节点地址传给 Redisson，且地址为空时静默产出空 client 列表，
 * 导致锁不可用且报错晦涩。
 *
 * <p>修复后：splitNodes 对 null/空白/仅逗号 一律抛 IllegalArgumentException（fail fast），
 * 多节点串 trim 并过滤空串后逐节点返回。
 *
 * <p>无修复则失败：旧实现无 splitNodes 方法，下方断言无从谈起；空地址会静默返回空列表而非抛异常。
 */
class RedissonConfigPropertiesSplitNodesTest {

    @Test
    void splitNodes_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> RedissonConfigProperties.splitNodes(null));
    }

    @Test
    void splitNodes_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> RedissonConfigProperties.splitNodes("   "));
    }

    @Test
    void splitNodes_onlyCommas_throws() {
        assertThrows(IllegalArgumentException.class, () -> RedissonConfigProperties.splitNodes(" , , "));
    }

    @Test
    void splitNodes_singleNode() {
        List<String> nodes = RedissonConfigProperties.splitNodes("127.0.0.1:6379");
        assertEquals(1, nodes.size());
        assertEquals("127.0.0.1:6379", nodes.get(0));
    }

    @Test
    void splitNodes_multiNode_trimsAndFiltersEmpty() {
        List<String> nodes = RedissonConfigProperties.splitNodes(" 127.0.0.1:7000 , 127.0.0.1:7001 ,, 127.0.0.1:7002 ");
        assertEquals(3, nodes.size());
        assertEquals("127.0.0.1:7000", nodes.get(0));
        assertEquals("127.0.0.1:7001", nodes.get(1));
        assertEquals("127.0.0.1:7002", nodes.get(2));
    }
}
