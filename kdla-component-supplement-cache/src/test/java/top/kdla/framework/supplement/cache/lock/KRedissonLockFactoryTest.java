package top.kdla.framework.supplement.cache.lock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import top.kdla.framework.supplement.cache.lock.properties.KRedissonConfigProperties;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归 F-01：活路径 KRedissonLockFactory.redissons() 须委托 properties.redissons()，
 * 全模式 fail-fast/脱敏修复须落在生产工厂而非死副本。
 * 无修复则失败：旧工厂自带 redissons() 仅 SINGLE/REPLICATE，其余模式静默零节点 RedLock。
 * 用 mock client，不触网。
 */
class KRedissonLockFactoryTest {

    @Test
    void redissons_delegatesToProperties() {
        RedissonClient c1 = Mockito.mock(RedissonClient.class);
        RedissonClient c2 = Mockito.mock(RedissonClient.class);
        KRedissonConfigProperties props = Mockito.mock(KRedissonConfigProperties.class);
        Mockito.when(props.redissons()).thenReturn(Arrays.asList(c1, c2));

        KRedissonLockFactory factory = new KRedissonLockFactory(props, "app");

        Mockito.verify(props).redissons();
        @SuppressWarnings("unchecked")
        List<RedissonClient> clients = (List<RedissonClient>) ReflectionTestUtils.getField(factory, "redissionClientList");
        assertThat(clients).containsExactly(c1, c2);
    }

    @Test
    void getLock_buildsRedLockFromDelegatedClientsWithPrefix() {
        RedissonClient c1 = Mockito.mock(RedissonClient.class);
        RLock lock1 = Mockito.mock(RLock.class);
        Mockito.when(c1.getLock("app:lock:biz")).thenReturn(lock1);
        KRedissonConfigProperties props = Mockito.mock(KRedissonConfigProperties.class);
        Mockito.when(props.redissons()).thenReturn(Arrays.asList(c1));

        KRedissonLockFactory factory = new KRedissonLockFactory(props, "app");
        RLock result = factory.getLock("biz");

        assertThat(result).isInstanceOf(RedissonRedLock.class);
        Mockito.verify(c1).getLock("app:lock:biz");
    }

    @Test
    void failFastFromProperties_propagatesThroughFactory() {
        KRedissonConfigProperties props = Mockito.mock(KRedissonConfigProperties.class);
        Mockito.when(props.redissons()).thenThrow(new IllegalArgumentException("kdla.redisson.address 不能为空"));

        assertThatThrownBy(() -> new KRedissonLockFactory(props, "app"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}