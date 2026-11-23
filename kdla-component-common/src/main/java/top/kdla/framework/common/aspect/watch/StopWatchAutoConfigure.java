package top.kdla.framework.common.aspect.watch;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(name = "kdla.config.stop-watch.enabled",
                       havingValue = "true", matchIfMissing = true)
@Slf4j
public class StopWatchAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public StopWatchWrapperAspect stopWatchWrapperAspect() {
        if (log.isInfoEnabled()) {
            log.info("StopWatchWrapperAspect init Bean");
        }
        return new StopWatchWrapperAspect();
    }
}
