package top.kdla.framework.common.aspect.watch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
public class StopWatchAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public StopWatchWrapperAspect stopWatchWrapperAspect() {
        return new StopWatchWrapperAspect();
    }
}
