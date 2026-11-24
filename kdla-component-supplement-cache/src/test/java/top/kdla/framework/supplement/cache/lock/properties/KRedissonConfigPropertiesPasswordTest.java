package top.kdla.framework.supplement.cache.lock.properties;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import top.kdla.framework.supplement.cache.lock.RedisClusterType;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：密码须在各模式自己的配置链内联设置。
 *
 * <p>无修复则失败：旧实现在 useMasterSlaveServers()/useClusterServers() 之后再调
 * config.useSingleServer().setPassword(...)，Redisson 抛 IllegalStateException，
 * 带密码的主从/集群模式启动即炸。
 */
class KRedissonConfigPropertiesPasswordTest {

    private KRedissonConfigProperties props(RedisClusterType type, String address) {
        KRedissonConfigProperties p = new KRedissonConfigProperties();
        p.setType(type);
        p.setAddress(address);
        p.setPassword("secret");
        // 用轻量 codec，避免测试依赖 jackson，聚焦密码设置路径
        p.setCodec("org.redisson.client.codec.StringCodec");
        return p;
    }

    private Config invoke(KRedissonConfigProperties p, String methodName, Class<?>[] types, Object[] args) throws Exception {
        Method m = KRedissonConfigProperties.class.getDeclaredMethod(methodName, types);
        m.setAccessible(true);
        try {
            return (Config) m.invoke(p, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    @Test
    void cluster_withPassword_setsOnClusterConfigNotSingleServer() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.CLUSTER, "127.0.0.1:7000,127.0.0.1:7001");

        Config config = invoke(p, "configCluster", new Class<?>[0], new Object[0]);

        assertEquals("secret", config.useClusterServers().getPassword(), "密码须落在集群配置上");
    }

    @Test
    void masterSlave_withPassword_setsOnMasterSlaveConfigNotSingleServer() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.MASTER_SLAVE, "127.0.0.1:6379");

        Config config = invoke(p, "configMasterSlave", new Class<?>[0], new Object[0]);

        assertEquals("secret", config.useMasterSlaveServers().getPassword(), "密码须落在主从配置上");
    }

    @Test
    void single_withPassword_setsOnSingleServer() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.SINGLE, "127.0.0.1:6379");

        Config config = invoke(p, "configSingleNode", new Class<?>[]{String.class, String.class},
                new Object[]{"127.0.0.1:6379", "secret"});

        assertEquals("secret", config.useSingleServer().getPassword());
    }

    @Test
    void sentinel_withPassword_setsOnSentinelConfig() throws Exception {
        KRedissonConfigProperties p = props(RedisClusterType.SENTINEL, "127.0.0.1:26379,127.0.0.1:26380");
        p.setMasterName("mymaster");

        Config config = invoke(p, "configSentinel", new Class<?>[0], new Object[0]);

        assertEquals("secret", config.useSentinelServers().getPassword(), "密码须落在哨兵配置上");
        assertEquals("mymaster", config.useSentinelServers().getMasterName(), "masterName须落在哨兵配置上");
    }
}