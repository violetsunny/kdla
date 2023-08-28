package top.kdla.framework.domain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
public class DomainAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(ApplicationContextHelp.class)
    public ApplicationContextHelp applicationContextHelper() {
        return new ApplicationContextHelp();
    }
}
