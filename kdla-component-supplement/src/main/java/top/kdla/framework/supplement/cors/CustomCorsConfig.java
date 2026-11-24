package top.kdla.framework.supplement.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(CorsFilter.class)
@ConditionalOnProperty(name = "kdla.config.cors.enabled", havingValue = "true", matchIfMissing = false)
public class CustomCorsConfig {

    //origin白名单，未配置默认拒绝一切跨域；支持精确origin与通配pattern
    @Value("${kdla.config.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Value("${kdla.config.cors.allow-credentials:false}")
    private boolean allowCredentials;

    /**
     * 跨域配置
     */
    @Bean
    @ConditionalOnMissingBean({CorsFilter.class})
    public CorsFilter unifyAuthCorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", this.buildConfig());
        return new CorsFilter(source);
    }

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return corsConfiguration;
        }
        if (allowCredentials && allowedOrigins.contains(CorsConfiguration.ALL)) {
            throw new IllegalStateException("kdla.config.cors.allow-credentials=true时allowed-origins禁止配置*");
        }
        corsConfiguration.setAllowedOriginPatterns(allowedOrigins);
        corsConfiguration.setAllowCredentials(allowCredentials);
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
        return corsConfiguration;
    }

}
