package top.kdla.framework.supplement.mqtt.vertx;

import io.vertx.mqtt.MqttClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全审计 D-07 回归测试：Vertx MQTT 无 TLS 开关、"-1" 哨兵被当真实凭据发送。
 *
 * <p>修复后：新增 kdla.mqtt.ssl（默认 false）；username/password 默认空，
 * 空白或 "-1" 一律不 set；真实凭据正常装载。
 *
 * <p>无修复则失败：旧代码 options.getUsername()=="-1"、无 setSsl。
 */
class VertxMqttConfigureSecurityTest {

    @Test
    void sentinelCredentials_notSent() throws Exception {
        VertxMqttConfigure configure = newConfigure("-1", "-1", false);
        MqttClientOptions options = configure.buildClientOptions();
        assertNull(options.getUsername(), "\"-1\"哨兵不得作为用户名发送");
        assertNull(options.getPassword(), "\"-1\"哨兵不得作为密码发送");
    }

    @Test
    void blankCredentials_notSent() throws Exception {
        VertxMqttConfigure configure = newConfigure("", "", false);
        MqttClientOptions options = configure.buildClientOptions();
        assertNull(options.getUsername());
        assertNull(options.getPassword());
    }

    @Test
    void realCredentials_sent() throws Exception {
        VertxMqttConfigure configure = newConfigure("device-user", "device-pass", false);
        MqttClientOptions options = configure.buildClientOptions();
        assertEquals("device-user", options.getUsername());
        assertEquals("device-pass", options.getPassword());
    }

    @Test
    void sslEnabled_optionSet() throws Exception {
        VertxMqttConfigure configure = newConfigure("u", "p", true);
        assertTrue(configure.buildClientOptions().isSsl());
    }

    @Test
    void sslDefault_false() throws Exception {
        Field f = VertxMqttConfigure.class.getDeclaredField("ssl");
        assertEquals("${kdla.mqtt.ssl:false}", f.getAnnotation(Value.class).value());
    }

    @Test
    void credentialDefaults_empty() throws Exception {
        assertEquals("${kdla.mqtt.username:}",
                VertxMqttConfigure.class.getDeclaredField("username").getAnnotation(Value.class).value());
        assertEquals("${kdla.mqtt.password:}",
                VertxMqttConfigure.class.getDeclaredField("password").getAnnotation(Value.class).value());
    }

    private static VertxMqttConfigure newConfigure(String username, String password, boolean ssl) throws Exception {
        VertxMqttConfigure configure = new VertxMqttConfigure();
        setField(configure, "clientId", "test-client");
        setField(configure, "username", username);
        setField(configure, "password", password);
        setField(configure, "acktimeout", 60000);
        setField(configure, "ssl", ssl);
        return configure;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
