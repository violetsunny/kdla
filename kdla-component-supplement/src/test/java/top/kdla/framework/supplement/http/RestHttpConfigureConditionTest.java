package top.kdla.framework.supplement.http;

import org.apache.hc.client5.http.classic.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归 A：RestHttpConfigure 补 @ConditionalOnClass 钉住 provided 的 httpclient5 HttpClient。
 * 无修复则失败：旧仅钉 RestTemplate，隐藏 HttpClient 后配置仍加载，触发 NoClassDefFoundError 致上下文失败。
 */
class RestHttpConfigureConditionTest {

    @Test
    void httpClient5Absent_configSkipped() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestHttpConfigure.class))
                .withPropertyValues("kdla.config.rest-http.enabled=true")
                .withClassLoader(new FilteredClassLoader(HttpClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(RestTemplate.class);
                    assertThat(context).doesNotHaveBean(BaseHttpClient.class);
                });
    }
}