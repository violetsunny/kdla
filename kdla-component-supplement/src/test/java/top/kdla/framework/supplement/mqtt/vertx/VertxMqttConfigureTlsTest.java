package top.kdla.framework.supplement.mqtt.vertx;

import io.vertx.mqtt.MqttClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/** D-07 剩余项回归：MQTTS 须可配私有 CA 信任库，且 hostname 校验默认开启、绝不 trust-all。 */
class VertxMqttConfigureTlsTest {

    @Test
    void hostnameVerification_enabledByDefault() throws Exception {
        MqttClientOptions options = newConfigure(true, "", "", true).buildClientOptions();
        assertThat(options.getHostnameVerificationAlgorithm())
                .isEqualTo("HTTPS");
    }

    @Test
    void vertxMqttDefault_hasNoHostnameVerification() {
        //证明本修复非冗余：Vertx 原生 MqttClientOptions 默认不校验 hostname
        assertThat(new MqttClientOptions().getHostnameVerificationAlgorithm()).isNullOrEmpty();
    }

    @Test
    void hostnameVerification_disabledOnlyWhenExplicitlyConfigured() throws Exception {
        MqttClientOptions options = newConfigure(true, "", "", false).buildClientOptions();
        assertThat(options.getHostnameVerificationAlgorithm()).isEmpty();
    }

    @Test
    void trustAll_neverEnabled() throws Exception {
        assertThat(newConfigure(true, "/tmp/ca.jks", "secret", true).buildClientOptions().isTrustAll()).isFalse();
        assertThat(newConfigure(true, "", "", false).buildClientOptions().isTrustAll()).isFalse();
    }

    @Test
    void truststore_notConfiguredUsesJvmDefault() throws Exception {
        MqttClientOptions options = newConfigure(true, "", "", true).buildClientOptions();
        assertThat(options.isSsl()).isTrue();
        assertThat(options.getTrustStoreOptions()).isNull();
    }

    @Test
    void truststore_configuredApplied() throws Exception {
        MqttClientOptions options = newConfigure(true, "/opt/certs/broker.jks", "changeit", true).buildClientOptions();
        assertThat(options.getTrustStoreOptions()).isNotNull();
        assertThat(options.getTrustStoreOptions().getPath()).isEqualTo("/opt/certs/broker.jks");
        assertThat(options.getTrustStoreOptions().getPassword()).isEqualTo("changeit");
    }

    @Test
    void truststore_blankPasswordNotSet() throws Exception {
        MqttClientOptions options = newConfigure(true, "/opt/certs/broker.jks", "", true).buildClientOptions();
        assertThat(options.getTrustStoreOptions().getPath()).isEqualTo("/opt/certs/broker.jks");
        assertThat(options.getTrustStoreOptions().getPassword()).isNull();
    }

    @Test
    void truststore_ignoredWhenSslDisabled() throws Exception {
        MqttClientOptions options = newConfigure(false, "/opt/certs/broker.jks", "changeit", true).buildClientOptions();
        assertThat(options.isSsl()).isFalse();
        assertThat(options.getTrustStoreOptions()).isNull();
    }

    @Test
    void annotationDefaults() throws Exception {
        assertThat(field("truststore").getAnnotation(Value.class).value())
                .isEqualTo("${kdla.mqtt.ssl.truststore:}");
        assertThat(field("truststorePassword").getAnnotation(Value.class).value())
                .isEqualTo("${kdla.mqtt.ssl.truststore-password:}");
        assertThat(field("verifyHost").getAnnotation(Value.class).value())
                .isEqualTo("${kdla.mqtt.ssl.verify-host:true}");
    }

    private static Field field(String name) throws Exception {
        Field f = VertxMqttConfigure.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static VertxMqttConfigure newConfigure(boolean ssl, String truststore,
                                                   String truststorePassword, boolean verifyHost) throws Exception {
        VertxMqttConfigure configure = new VertxMqttConfigure();
        set(configure, "clientId", "test-client");
        set(configure, "acktimeout", 60000);
        set(configure, "ssl", ssl);
        set(configure, "truststore", truststore);
        set(configure, "truststorePassword", truststorePassword);
        set(configure, "verifyHost", verifyHost);
        return configure;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
