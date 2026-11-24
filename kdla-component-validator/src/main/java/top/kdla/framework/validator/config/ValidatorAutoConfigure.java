package top.kdla.framework.validator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kdla.framework.validator.CommonValidator;


/**
 * CommonValidator 自动配置，默认开启，宿主配置 kdla.config.validator.enabled=false 关闭。
 *
 * @author kll
 */
@Configuration
//守实现类而非API：下游只有validation-api无hibernate-validator时退让，避免构造CommonValidator启动失败
@ConditionalOnClass(name = "org.hibernate.validator.HibernateValidator")
@ConditionalOnProperty(name = "kdla.config.validator.enabled", havingValue = "true", matchIfMissing = true)
public class ValidatorAutoConfigure {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(CommonValidator.class)
    public CommonValidator commonValidator() {
        return new CommonValidator();
    }

}
