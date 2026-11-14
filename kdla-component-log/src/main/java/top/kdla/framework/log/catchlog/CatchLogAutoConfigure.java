package top.kdla.framework.log.catchlog;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 方法日志切面自动配置
 * 
 * <p>默认开启,可显式关闭:</p>
 * <pre>{@code
 * kdla:
 *   config:
 *     catch-log:
 *       enabled: false
 * }</pre>
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(name = "kdla.config.catch-log.enabled",
                       havingValue = "true", matchIfMissing = true)
public class CatchLogAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(CatchLogAspect.class)
    public CatchLogAspect catchLogAspect() {
        return new CatchLogAspect();
    }
}
