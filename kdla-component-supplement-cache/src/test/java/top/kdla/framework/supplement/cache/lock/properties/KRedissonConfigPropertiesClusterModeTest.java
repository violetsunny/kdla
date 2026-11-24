package top.kdla.framework.supplement.cache.lock.properties;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import top.kdla.framework.supplement.cache.lock.RedisClusterType;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归 F-01：MASTER_SLAVE/CLUSTER/SENTINEL 须正确解析多节点（仅构建 Config，不触网）。
 * 无修复则失败：旧 configMasterSlave 把整串既当 master 又当 slave；SENTINEL 未实现。
 */
class KRedissonConfigPropertiesClusterModeTest {

    private KRedissonConfigProperties props(RedisClusterType type, String address) {
        KRedissonConfigProperties p = new KRedissonConfigProperties();
        p.setType(type);
        p.setAddress(address);
        p.setCodec("org.redisson.client.codec.StringCodec");
        return p;
    }

    private Config invoke(KRedissonConfigProperties p, String methodName) throws Exception {
        Method m = KRedissonConfigProperties.class.getDeclaredMethod(methodName);
        m.setAccessible(true);
        try {
            return (Config) m.invoke(p);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    @Test
    void masterSlave_firstNodeMasterRestSlaves() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.MASTER_SLAVE, "m:6379,s1:6379,s2:6379");
        Config config = invoke(p, "configMasterSlave");
        assertThat(config.useMasterSlaveServers().getMasterAddress()).contains("m:6379");
        assertThat(config.useMasterSlaveServers().getSlaveAddresses()).hasSize(2);
    }

    @Test
    void masterSlave_singleNode_masterOnlyNoSlave() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.MASTER_SLAVE, "m:6379");
        Config config = invoke(p, "configMasterSlave");
        assertThat(config.useMasterSlaveServers().getMasterAddress()).contains("m:6379");
        assertThat(config.useMasterSlaveServers().getSlaveAddresses()).isEmpty();
    }

    @Test
    void cluster_multiNode_allAdded() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.CLUSTER, "n1:7000,n2:7001,n3:7002");
        Config config = invoke(p, "configCluster");
        assertThat(config.useClusterServers().getNodeAddresses()).hasSize(3);
    }

    @Test
    void sentinel_masterNameAndSentinelNodes() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.SENTINEL, "s1:26379,s2:26379");
        p.setMasterName("mymaster");
        Config config = invoke(p, "configSentinel");
        assertThat(config.useSentinelServers().getMasterName()).isEqualTo("mymaster");
        assertThat(config.useSentinelServers().getSentinelAddresses()).hasSize(2);
    }
}