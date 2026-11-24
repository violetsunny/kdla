package top.kdla.framework.supplement.cache.lock.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import top.kdla.framework.supplement.cache.lock.KDistributeLockFactory;
import top.kdla.framework.supplement.cache.lock.properties.KRedissonConfigProperties;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归 C3：app.id 缺失时锁前缀回落固定值 "kdla"（旧实现用随机雪花ID，多实例锁 key 不一致致互斥失效）。
 * 无修复则失败：旧前缀为随机值，lockFullPath 不等于 "kdla:lock:"。
 */
class KRedissonAutoConfigureLockPrefixTest {

    private KDistributeLockFactory buildFactory(String appId) {
        KRedissonAutoConfigure configure = new KRedissonAutoConfigure();
        // 工厂已委托 properties.redissons()，mock 其返回空列表即控制活路径，避免真实 Redisson.create 连库
        KRedissonConfigProperties config = Mockito.mock(KRedissonConfigProperties.class);
        Mockito.when(config.redissons()).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(configure, "config", config);
        ReflectionTestUtils.setField(configure, "appId", appId);
        return configure.distributeLockFactory();
    }

    @Test
    void appIdBlank_prefixFallsBackToKdla() {
        KDistributeLockFactory factory = buildFactory("");
        assertThat(ReflectionTestUtils.getField(factory, "lockFullPath")).isEqualTo("kdla:lock:");
    }

    @Test
    void appIdPresent_prefixUsesAppId() {
        KDistributeLockFactory factory = buildFactory("my-app");
        assertThat(ReflectionTestUtils.getField(factory, "lockFullPath")).isEqualTo("my-app:lock:");
    }
}