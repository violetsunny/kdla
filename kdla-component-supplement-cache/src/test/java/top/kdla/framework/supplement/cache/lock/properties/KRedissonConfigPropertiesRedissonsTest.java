package top.kdla.framework.supplement.cache.lock.properties;

import org.junit.jupiter.api.Test;
import top.kdla.framework.supplement.cache.lock.RedisClusterType;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：redissons() 各分支须接线 splitNodes 做 fail-fast 校验。
 *
 * <p>无修复则失败：旧实现 SINGLE/MASTER_SLAVE 不校验地址、REPLICATE 用裸 split 且无密码数量校验、
 * CLUSTER 把整串当单节点、SENTINEL 缺 master-name 不校验。以下用例均在 Redisson.create 之前抛出，不触网。
 */
class KRedissonConfigPropertiesRedissonsTest {

    private KRedissonConfigProperties props(RedisClusterType type, String address, String password) {
        KRedissonConfigProperties p = new KRedissonConfigProperties();
        p.setType(type);
        p.setAddress(address);
        p.setPassword(password);
        return p;
    }

    @Test
    void single_blankAddress_failFast() {
        KRedissonConfigProperties p = props(RedisClusterType.SINGLE, "   ", null);
        assertThrows(IllegalArgumentException.class, p::redissons);
    }

    @Test
    void masterSlave_blankAddress_failFast() {
        KRedissonConfigProperties p = props(RedisClusterType.MASTER_SLAVE, " , , ", null);
        assertThrows(IllegalArgumentException.class, p::redissons);
    }

    @Test
    void replicate_blankAddress_failFast() {
        KRedissonConfigProperties p = props(RedisClusterType.REPLICATE, null, null);
        assertThrows(IllegalArgumentException.class, p::redissons);
    }

    @Test
    void replicate_passwordCountMismatch_failFast() {
        KRedissonConfigProperties p = props(RedisClusterType.REPLICATE,
                "127.0.0.1:7000,127.0.0.1:7001", "onlyOnePassword");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, p::redissons);
        assertTrue(ex.getMessage().contains("不匹配"), "须提示密码数量与节点数量不匹配");
    }

    @Test
    void cluster_blankAddress_failFast() {
        KRedissonConfigProperties p = props(RedisClusterType.CLUSTER, " , ", null);
        assertThrows(IllegalArgumentException.class, p::redissons,
                "configCluster须接线splitNodes逐节点校验，而非把整串当单节点");
    }

    @Test
    void sentinel_blankMasterName_failFast() {
        KRedissonConfigProperties p = props(RedisClusterType.SENTINEL, "127.0.0.1:26379", null);
        assertThrows(IllegalArgumentException.class, p::redissons,
                "SENTINEL必须配置master-name，缺失须fail fast而非静默空列表");
    }

    @Test
    void sentinel_blankAddress_failFast() {
        KRedissonConfigProperties p = props(RedisClusterType.SENTINEL, " , ", null);
        p.setMasterName("mymaster");
        assertThrows(IllegalArgumentException.class, p::redissons,
                "SENTINEL地址空白须fail fast");
    }
}
