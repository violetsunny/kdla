package top.kdla.framework.supplement.cache.cache.config;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import top.kdla.framework.supplement.cache.cache.KdlaCafCache;
import top.kdla.framework.supplement.cache.cache.MachineIdHelp;
import top.kdla.framework.supplement.cache.cache.SpringRedisHelp;

import jakarta.annotation.PreDestroy;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 回归 C1/C2：caffeineCacheManager 用 ObjectProvider 替代 @Autowired List（无 KdlaCafCache bean 时不得启动失败）；
 * @PreDestroy 方法必须无参。无修复则失败：旧 List 注入空集合抛 UnsatisfiedDependencyException，旧 destroy 带 1 参。
 */
class CacheAutoConfigureRegressionTest {

    @Configuration
    static class MockCollaborators {
        @Bean
        RedisTemplate<?, ?> redisTemplate() {
            return mock(RedisTemplate.class);
        }

        @Bean
        SpringRedisHelp springRedisHelp() {
            return mock(SpringRedisHelp.class);
        }

        @Bean
        MachineIdHelp machineIdHelp() {
            return mock(MachineIdHelp.class);
        }

        @Bean
        SnowflakeGenerator snowflakeGenerator() {
            return mock(SnowflakeGenerator.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheAutoConfigure.class))
            .withUserConfiguration(MockCollaborators.class)
            .withPropertyValues("kdla.cache.enabled=true");

    @Test
    void noCaffeineBean_contextLoads_cacheManagerPresent() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CacheManager.class);
        });
    }

    @Test
    void withCaffeineBean_cacheManagerContainsIt() {
        runner.withBean("testCaf", KdlaCafCache.class, () -> new KdlaCafCache<>("testCaf", 60L, 16, 100))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CacheManager.class).getCache("testCaf")).isNotNull();
                });
    }

    @Test
    void preDestroyMethodIsNoArg() throws NoSuchMethodException {
        Method destroy = CacheAutoConfigure.class.getDeclaredMethod("destroy");
        assertThat(destroy.getParameterCount()).isZero();
        assertThat(destroy.isAnnotationPresent(PreDestroy.class)).isTrue();
    }
}