package top.kdla.framework.supplement.monitor.prometheus.annotation;

import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import top.kdla.framework.supplement.monitor.prometheus.PushGatewayManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrometheusPointAspectTest {

    static class SampleService {
        @PrometheusCounter(name = "kdla_test_counter", help = "test", type = "api")
        public String counted() {
            return "ok";
        }
    }

    @Test
    void repeatedInvocation_counterAccumulatesWithoutDuplicateRegistration() {
        CollectorRegistry registry = new CollectorRegistry();
        PushGatewayManager manager = mock(PushGatewayManager.class);
        when(manager.getRegistry()).thenReturn(registry);

        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new PrometheusPointAspect(manager));
        SampleService proxy = factory.getProxy();

        assertThat(proxy.counted()).isEqualTo("ok");
        assertThat(proxy.counted()).isEqualTo("ok");

        Double value = registry.getSampleValue("kdla_test_counter_total",
                new String[]{"type"}, new String[]{"api"});
        assertThat(value).isEqualTo(2.0);
    }
}