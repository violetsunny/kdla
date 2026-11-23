package top.kdla.framework.supplement.fieldvaluefinder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kll
 * @date 2022/2/16
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "kdla.config.field-value-find.enabled", havingValue = "true", matchIfMissing = true)
public class FieldValueFindConfigure {

    @Bean
    @ConditionalOnMissingBean(FieldValueFindHelp.class)
    public FieldValueFindHelp fieldValueFindHelper() {
        if (log.isInfoEnabled()) {
            log.info("FieldValueFindHelp init Bean");
        }
        return new FieldValueFindHelp();
    }

}
