package top.kdla.framework.log.catchlog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归：@ConditionalOnClass与kdla.config.catch-log.enabled开关被删，宿主无法关闭切面。
 * 修复后：默认开启，显式置false关闭。
 */
class CatchLogAutoConfigureTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CatchLogAutoConfigure.class));

    @Test
    void defaultEnabled_registersAspect() {
        runner.run(context -> assertThat(context).hasSingleBean(CatchLogAspect.class));
    }

    @Test
    void disabledByProperty_noAspectBean() {
        // 修复前：开关缺失，enabled=false仍注册切面
        runner.withPropertyValues("kdla.config.catch-log.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CatchLogAspect.class));
    }
}