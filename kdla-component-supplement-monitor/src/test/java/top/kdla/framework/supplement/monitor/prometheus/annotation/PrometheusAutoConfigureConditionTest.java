package top.kdla.framework.supplement.monitor.prometheus.annotation;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.kdla.framework.supplement.monitor.prometheus.PushGatewayManager;

import static org.assertj.core.api.Assertions.assertThat;

class PrometheusAutoConfigureConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PrometheusAutoConfigure.class))
            .withPropertyValues("spring.application.name=test-app", "spring.profiles.active=test");

    @Test
    void prometheusAbsent_contextStartsWithoutBeans() {
        runner.withClassLoader(new FilteredClassLoader(CollectorRegistry.class, PushGateway.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(PushGatewayManager.class);
                    assertThat(context).doesNotHaveBean(PrometheusPointAspect.class);
                });
    }

    @Test
    void prometheusPresent_bothBeansRegistered() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PushGatewayManager.class);
            assertThat(context).hasSingleBean(PrometheusPointAspect.class);
        });
    }
}