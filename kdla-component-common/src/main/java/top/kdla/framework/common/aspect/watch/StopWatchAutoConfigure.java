package top.kdla.framework.common.aspect.watch;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 方法执行时间监控自动配置
 *
 * <p>默认开启,可显式关闭:</p>
 * <pre>{@code
 * kdla:
 *   config:
 *     stop-watch:
 *       enabled: false
 * }</pre>
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
@Configuration
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(name = "kdla.config.stop-watch.enabled",
                       havingValue = "true", matchIfMissing = true)
public class StopWatchAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public StopWatchWrapperAspect stopWatchWrapperAspect() {
        return new StopWatchWrapperAspect();
    }
}
