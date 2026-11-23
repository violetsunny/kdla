package top.kdla.framework.supplement.mqtt.vertx;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 回归 B：vertxMqttClient 恢复 @ConditionalOnMissingBean(VertxMqttClient.class)，宿主自定义时自动 Bean 退让。
 * 无修复则失败：旧无 COBM，自动 Bean 与用户 Bean 并存，hasSingleBean 不通过。
 * 提供 mock MqttClient/Vertx 令 mqttClient/vertx 自动 Bean 退让，避免真实连库。
 */
class VertxMqttConfigureConditionTest {

    @Test
    void userVertxMqttClientPresent_autoBeanBacksOff() {
        VertxMqttClient userClient = new VertxMqttClient(mock(MqttClient.class));
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VertxMqttConfigure.class))
                .withPropertyValues("kdla.config.mqtt.enabled=true")
                .withBean(MqttClient.class, () -> mock(MqttClient.class))
                .withBean(Vertx.class, () -> mock(Vertx.class))
                .withBean("userVertxMqttClient", VertxMqttClient.class, () -> userClient)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(VertxMqttClient.class);
                    assertThat(context.getBean(VertxMqttClient.class)).isSameAs(userClient);
                });
    }
}