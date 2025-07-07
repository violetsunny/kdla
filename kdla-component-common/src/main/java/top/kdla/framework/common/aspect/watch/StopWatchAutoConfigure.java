package top.kdla.framework.common.aspect.watch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
@Slf4j
public class StopWatchAutoConfigure {

    @Bean
    public StopWatchWrapperAspect stopWatchWrapperAspect() {
        if (log.isInfoEnabled()) {
            log.info("StopWatchWrapperAspect init Bean");
        }
        return new StopWatchWrapperAspect();
    }
}
