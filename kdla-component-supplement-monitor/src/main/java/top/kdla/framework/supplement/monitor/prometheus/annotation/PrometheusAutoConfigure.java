package top.kdla.framework.supplement.monitor.prometheus.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import top.kdla.framework.common.aspect.watch.StopWatchWrapperAspect;
import top.kdla.framework.supplement.monitor.prometheus.PushGatewayManager;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
public class PrometheusAutoConfigure {

    //条件必须钉第三方类：钉自有类恒真，下游缺prometheus依赖时启动即炸
    @Bean
    @ConditionalOnMissingBean(PushGatewayManager.class)
    @ConditionalOnClass({CollectorRegistry.class, PushGateway.class})
    public PushGatewayManager pushGatewayManager() {
        return new PushGatewayManager();
    }

    @Bean
    @ConditionalOnMissingBean(PrometheusPointAspect.class)
    @ConditionalOnClass({CollectorRegistry.class, PushGateway.class})
    public PrometheusPointAspect prometheusPointAspect(@Autowired PushGatewayManager pushGatewayManager) {
        return new PrometheusPointAspect(pushGatewayManager);
    }
}
