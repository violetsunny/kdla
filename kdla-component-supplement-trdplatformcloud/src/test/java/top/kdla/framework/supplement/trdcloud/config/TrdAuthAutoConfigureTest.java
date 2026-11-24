package top.kdla.framework.supplement.trdcloud.config;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class TrdAuthAutoConfigureTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TrdAuthAutoConfigure.class));

    @Test
    void registersByDefaultWithTrdUrlPattern() {
        // B-04：默认开启（matchIfMissing=true），覆盖全部 /trd/* 端点
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(FilterRegistrationBean.class);
            FilterRegistrationBean<?> registration = ctx.getBean(FilterRegistrationBean.class);
            assertThat(registration.getUrlPatterns()).contains("/trd/*");
            assertThat(registration.getFilter()).isInstanceOf(TrdBladeAuthFilter.class);
        });
    }

    @Test
    void backsOffWhenExplicitlyDisabled() {
        runner.withPropertyValues("kdla.trd.auth.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class));
    }

    @Test
    void backsOffWhenServletApiMissing() {
        runner.withClassLoader(new FilteredClassLoader(Filter.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class));
    }
}
