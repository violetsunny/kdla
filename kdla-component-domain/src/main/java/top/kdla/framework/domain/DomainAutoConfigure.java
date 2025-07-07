package top.kdla.framework.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
@Slf4j
public class DomainAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(ApplicationContextHelp.class)
    public ApplicationContextHelp applicationContextHelper() {
        if (log.isInfoEnabled()) {
            log.info("ApplicationContextHelp init Bean");
        }
        return new ApplicationContextHelp();
    }
}
