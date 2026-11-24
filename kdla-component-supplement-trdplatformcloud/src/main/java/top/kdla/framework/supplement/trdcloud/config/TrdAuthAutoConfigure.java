package top.kdla.framework.supplement.trdcloud.config;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * trd blade-auth 准入自动配置，默认开启（fail-closed）。
 * 破坏性变更：下游访问 /trd/* 必须携带有效 blade-auth 头并配置 kdla.trd.jwt.sign-key；
 * 显式设置 kdla.trd.auth.enabled=false 可关闭，关闭方须自行提供等效鉴权。
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Filter.class)
@ConditionalOnProperty(name = "kdla.trd.auth.enabled", havingValue = "true", matchIfMissing = true)
public class TrdAuthAutoConfigure {

    @Bean
    public FilterRegistrationBean<TrdBladeAuthFilter> trdBladeAuthFilterRegistration() {
        FilterRegistrationBean<TrdBladeAuthFilter> registration = new FilterRegistrationBean<>(new TrdBladeAuthFilter());
        registration.addUrlPatterns("/trd/*");
        registration.setName("trdBladeAuthFilter");
        registration.setOrder(0);
        return registration;
    }
}