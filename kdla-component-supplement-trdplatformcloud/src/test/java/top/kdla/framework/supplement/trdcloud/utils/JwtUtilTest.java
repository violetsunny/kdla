package top.kdla.framework.supplement.trdcloud.utils;

import cn.hutool.jwt.JWTUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import top.kdla.framework.domain.ApplicationContextHelp;
import top.kdla.framework.exception.BizException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilTest {

    private static final String KEY = "unit-test-sign-key-0123456789abcdef";
    private static final String KEY_PROPERTY = "kdla.trd.jwt.sign-key";

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty(KEY_PROPERTY, KEY);
        injectContext(env);
    }

    @AfterEach
    void tearDown() {
        new ApplicationContextHelp().setApplicationContext(null);
    }

    private void injectContext(MockEnvironment env) {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getEnvironment()).thenReturn(env);
        new ApplicationContextHelp().setApplicationContext(ctx);
    }

    private String token(long expEpochSeconds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("account", "zhangsan");
        payload.put("exp", expEpochSeconds);
        return JWTUtil.createToken(payload, KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validTokenReturnsAccount() {
        String jwt = token(System.currentTimeMillis() / 1000 + 3600);
        assertThat(JwtUtil.getInfoFromToken("Bearer " + jwt, "account")).isEqualTo("zhangsan");
    }

    @Test
    void tamperedSignatureRejected() {
        String jwt = token(System.currentTimeMillis() / 1000 + 3600);
        String[] parts = jwt.split("\\.");
        char flipped = parts[2].charAt(0) == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + flipped + parts[2].substring(1);
        assertThatThrownBy(() -> JwtUtil.getInfoFromToken("Bearer " + tampered, "account"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void wrongKeyRejected() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("account", "zhangsan");
        payload.put("exp", System.currentTimeMillis() / 1000 + 3600);
        String jwt = JWTUtil.createToken(payload, "another-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> JwtUtil.getInfoFromToken("Bearer " + jwt, "account"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void expiredTokenRejected() {
        String jwt = token(System.currentTimeMillis() / 1000 - 3600);
        assertThatThrownBy(() -> JwtUtil.getInfoFromToken("Bearer " + jwt, "account"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void missingSignKeyRejected() {
        injectContext(new MockEnvironment());
        String jwt = token(System.currentTimeMillis() / 1000 + 3600);
        assertThatThrownBy(() -> JwtUtil.getInfoFromToken("Bearer " + jwt, "account"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void nullContextRejected() {
        new ApplicationContextHelp().setApplicationContext(null);
        assertThatThrownBy(() -> JwtUtil.getInfoFromToken("Bearer whatever", "account"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void malformedAuthRejected() {
        assertThatThrownBy(() -> JwtUtil.getInfoFromToken("garbage", "account"))
                .isInstanceOf(BizException.class);
    }

}