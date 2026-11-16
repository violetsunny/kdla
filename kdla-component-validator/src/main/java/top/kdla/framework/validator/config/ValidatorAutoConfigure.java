package top.kdla.framework.validator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kdla.framework.validator.CommonValidator;

import javax.validation.Validator;

/**
 * CommonValidator 自动配置，默认开启，宿主配置 kdla.config.validator.enabled=false 关闭。
 *
 * @author kll
 */
@Configuration
@ConditionalOnClass(Validator.class)
@ConditionalOnProperty(name = "kdla.config.validator.enabled", havingValue = "true", matchIfMissing = true)
public class ValidatorAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(CommonValidator.class)
    public CommonValidator commonValidator() {
        return new CommonValidator();
    }

}
