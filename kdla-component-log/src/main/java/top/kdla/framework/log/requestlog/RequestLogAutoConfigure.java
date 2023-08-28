package top.kdla.framework.log.requestlog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration(
        proxyBeanMethods = false
)
@Profile({"dev", "test"})
public class RequestLogAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(RequestLogAspect.class)
    public RequestLogAspect requestLogAspect() {
        return new RequestLogAspect();
    }
}
