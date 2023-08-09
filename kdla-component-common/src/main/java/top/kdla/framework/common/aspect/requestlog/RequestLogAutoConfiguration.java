package top.kdla.framework.common.aspect.requestlog;

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
public class RequestLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RequestLogAspect.class)
    public RequestLogAspect requestLogAspect() {
        return new RequestLogAspect();
    }
}
