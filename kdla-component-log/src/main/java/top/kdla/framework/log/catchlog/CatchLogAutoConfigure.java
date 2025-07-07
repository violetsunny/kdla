package top.kdla.framework.log.catchlog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
@Slf4j
public class CatchLogAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(CatchLogAspect.class)
    public CatchLogAspect catchLogAspect() {
        if (log.isInfoEnabled()) {
            log.info("CatchLogAspect init Bean");
        }
        return new CatchLogAspect();
    }
}
