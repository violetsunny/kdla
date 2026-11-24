package top.kdla.framework.validator.config;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.kdla.framework.validator.CommonValidator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归 C-1：validator 模块缺 AutoConfiguration.imports，ValidatorAutoConfigure 永不装配，
 * 下游 @Autowired CommonValidator 启动即 NoSuchBeanDefinitionException（测试直接 new 掩盖）。
 * 修复后：.imports 注册该类，默认开启，enabled=false 关闭，宿主自定义 bean 时退让。
 */
class ValidatorAutoConfigureRegistrationTest {

    private static final String IMPORTS =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
    private static final String FQN = "top.kdla.framework.validator.config.ValidatorAutoConfigure";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidatorAutoConfigure.class));

    @Test
    void importsResource_registersAutoConfigure() throws Exception {
        // 无修复则失败：.imports 缺失时资源为 null，自动配置不会被 Spring Boot 加载
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(IMPORTS)) {
            assertThat(in).as("validator 必须提供 AutoConfiguration.imports").isNotNull();
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content).contains(FQN);
        }
    }

    @Test
    void defaultEnabled_registersCommonValidator() {
        runner.run(context -> assertThat(context).hasSingleBean(CommonValidator.class));
    }

    @Test
    void disabledByProperty_noCommonValidatorBean() {
        runner.withPropertyValues("kdla.config.validator.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CommonValidator.class));
    }

    @Test
    void userBeanPresent_autoConfigureBacksOff() {
        CommonValidator userBean = new CommonValidator();
        runner.withBean(CommonValidator.class, () -> userBean)
                .run(context -> {
                    assertThat(context).hasSingleBean(CommonValidator.class);
                    assertThat(context.getBean(CommonValidator.class)).isSameAs(userBean);
                });
    }

    @Test
    void conditionGuardsImplementationNotApi() {
        ConditionalOnClass cond = ValidatorAutoConfigure.class.getAnnotation(ConditionalOnClass.class);
        // 修复前：value={jakarta.validation.Validator.class}守API，下游无实现时启动失败
        assertThat(cond.name()).contains("org.hibernate.validator.HibernateValidator");
        assertThat(cond.value()).isEmpty();
    }

    @Test
    void noHibernateImplementation_autoConfigureBacksOff() {
        runner.withClassLoader(new FilteredClassLoader(HibernateValidator.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CommonValidator.class);
                });
    }
}
