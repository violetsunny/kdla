package top.kdla.framework.supplement.cors;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全审计 D-02 回归测试：CustomCorsConfig 硬编码 allowCredentials=true + allowedOriginPatterns=ALL。
 *
 * <p>修复后：origin 白名单可配（kdla.config.cors.allowed-origins），默认空=拒绝一切跨域；
 * allowCredentials 可配默认 false；credentials=true 时禁止 * 通配。
 *
 * <p>无修复则失败：旧代码任意 Origin 均通过 checkOrigin 且 allowCredentials 恒 true。
 */
class CustomCorsConfigTest {

    @Test
    void emptyWhitelist_rejectsAnyOrigin() throws Exception {
        CorsConfiguration config = buildConfig(new CustomCorsConfig());
        assertNull(config.checkOrigin("https://evil.example"), "未配置白名单须拒绝一切跨域");
    }

    @Test
    void whitelist_allowsOnlyListedOrigins() throws Exception {
        CustomCorsConfig configure = new CustomCorsConfig();
        setField(configure, "allowedOrigins", List.of("https://app.example.com"));
        setField(configure, "allowCredentials", true);
        CorsConfiguration config = buildConfig(configure);
        assertEquals("https://app.example.com", config.checkOrigin("https://app.example.com"));
        assertNull(config.checkOrigin("https://evil.example"));
        assertTrue(config.getAllowCredentials());
    }

    @Test
    void credentialsWithWildcard_rejected() throws Exception {
        CustomCorsConfig configure = new CustomCorsConfig();
        setField(configure, "allowedOrigins", List.of(CorsConfiguration.ALL));
        setField(configure, "allowCredentials", true);
        assertThrows(IllegalStateException.class, () -> buildConfig(configure));
    }

    @Test
    void credentialsDefault_false() throws Exception {
        CustomCorsConfig configure = new CustomCorsConfig();
        setField(configure, "allowedOrigins", List.of("https://app.example.com"));
        CorsConfiguration config = buildConfig(configure);
        assertFalse(config.getAllowCredentials());
    }

    private static CorsConfiguration buildConfig(CustomCorsConfig configure) throws Exception {
        Method m = CustomCorsConfig.class.getDeclaredMethod("buildConfig");
        m.setAccessible(true);
        try {
            return (CorsConfiguration) m.invoke(configure);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw e;
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
