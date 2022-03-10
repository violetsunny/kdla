package top.kdla.framework.fieldvaluefinder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hjs
 * @date 2022/2/16
 */
@Slf4j
@Configuration
public class FieldValueFindConfig {

    @Bean
    public FieldValueFindHelper fieldValueFindHelper() {
        return new FieldValueFindHelper();
    }

    @Bean
    public FieldValueFindApplicationContextUtils fieldValueFindApplicationContextUtils() {
        return new FieldValueFindApplicationContextUtils();
    }
}
