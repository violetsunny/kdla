package top.kdla.framework.exception;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：SecurityAccessDeniedExceptionAdvice 通过 @Import + @ConditionalOnClass 按条件注册，
 * 宿主 classpath 无 spring-security 时不得注册（否则加载内部类会触发 NoClassDefFoundError），
 * 有 spring-security 时正常注册。
 */
class SecurityAccessDeniedAdviceConditionTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(UnifiedExceptionControllerAdvice.class);

    @Test
    void adviceRegistered_whenSpringSecurityOnClasspath() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(UnifiedExceptionControllerAdvice.SecurityAccessDeniedExceptionAdvice.class));
    }

    @Test
    void adviceSkipped_whenSpringSecurityAbsent() {
        runner.withClassLoader(new FilteredClassLoader(AccessDeniedException.class))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(UnifiedExceptionControllerAdvice.SecurityAccessDeniedExceptionAdvice.class));
    }
}
