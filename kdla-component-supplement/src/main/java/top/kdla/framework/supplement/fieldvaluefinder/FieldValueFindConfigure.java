package top.kdla.framework.supplement.fieldvaluefinder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kll
 * @date 2022/2/16
 */
@Slf4j
@Configuration
public class FieldValueFindConfigure {

    @Bean
    public FieldValueFindHelp fieldValueFindHelper() {
        if (log.isInfoEnabled()) {
            log.info("FieldValueFindHelp init Bean");
        }
        return new FieldValueFindHelp();
    }

}
