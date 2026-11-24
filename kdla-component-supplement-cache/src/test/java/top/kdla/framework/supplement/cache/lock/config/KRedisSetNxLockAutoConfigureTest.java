package top.kdla.framework.supplement.cache.lock.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import top.kdla.framework.supplement.cache.cache.SpringRedisHelp;
import top.kdla.framework.supplement.cache.lock.KRedisSetNxLock;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 回归 SET NX 锁装配：默认注册、app.id 前缀传递、开关关闭、无 SpringRedisHelp 不注册、
 * 下游自定义 Bean 让位、必须登记进 AutoConfiguration.imports（漏登记则生产永不加载）。
 */
class KRedisSetNxLockAutoConfigureTest {

    @Configuration
    static class MockRedisHelp {
        @Bean
        SpringRedisHelp springRedisHelp() {
            return mock(SpringRedisHelp.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KRedisSetNxLockAutoConfigure.class))
            .withUserConfiguration(MockRedisHelp.class);

    @Test
    void springRedisHelpPresent_beanRegisteredByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KRedisSetNxLock.class);
        });
    }

    @Test
    void appIdPresent_prefixUsesAppId() {
        runner.withPropertyValues("app.id=my-app").run(context ->
                assertThat(ReflectionTestUtils.getField(context.getBean(KRedisSetNxLock.class), "keyPrefix"))
                        .isEqualTo("my-app:lock:"));
    }

    @Test
    void appIdBlank_prefixFallsBackToKdla() {
        runner.run(context ->
                assertThat(ReflectionTestUtils.getField(context.getBean(KRedisSetNxLock.class), "keyPrefix"))
                        .isEqualTo("kdla:lock:"));
    }

    @Test
    void disabledByProperty_noBean() {
        runner.withPropertyValues("kdla.cache.setnx-lock.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(KRedisSetNxLock.class);
        });
    }

    @Test
    void noSpringRedisHelp_noBean() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KRedisSetNxLockAutoConfigure.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(KRedisSetNxLock.class);
                });
    }

    @Test
    void userDefinedBean_takesPrecedence() {
        runner.withBean("customLock", KRedisSetNxLock.class,
                        () -> new KRedisSetNxLock(mock(SpringRedisHelp.class), "custom"))
                .run(context -> {
                    assertThat(context).hasSingleBean(KRedisSetNxLock.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(KRedisSetNxLock.class), "keyPrefix"))
                            .isEqualTo("custom:lock:");
                });
    }

    @Test
    void registeredInAutoConfigurationImports() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
            assertThat(in).isNotNull();
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .contains(KRedisSetNxLockAutoConfigure.class.getName());
        }
    }
}
