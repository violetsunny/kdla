package top.kdla.framework.supplement.monitor.prometheus.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import top.kdla.framework.common.aspect.watch.StopWatchWrapperAspect;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
@EnableAspectJAutoProxy
public class PrometheusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PrometheusPointAspect.class)
    public PrometheusPointAspect prometheusPointAspect() {
        return new PrometheusPointAspect();
    }
}
