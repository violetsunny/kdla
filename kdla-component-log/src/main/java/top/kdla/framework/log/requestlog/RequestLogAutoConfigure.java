package top.kdla.framework.log.requestlog;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RequestLogAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(RequestLogAspect.class)
    public RequestLogAspect requestLogAspect() {
        if (log.isInfoEnabled()) {
            log.info("RequestLogAspect init Bean");
        }
        return new RequestLogAspect();
    }
}
