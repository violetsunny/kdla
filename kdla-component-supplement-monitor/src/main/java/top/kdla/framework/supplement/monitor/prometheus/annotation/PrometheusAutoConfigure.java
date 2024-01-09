package top.kdla.framework.supplement.monitor.prometheus.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import top.kdla.framework.common.aspect.watch.StopWatchWrapperAspect;
import top.kdla.framework.supplement.monitor.prometheus.PushGatewayManager;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
public class PrometheusAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(PrometheusPointAspect.class)
    @ConditionalOnClass({PushGatewayManager.class})
    public PrometheusPointAspect prometheusPointAspect(@Autowired PushGatewayManager pushGatewayManager) {
        return new PrometheusPointAspect(pushGatewayManager);
    }
}
