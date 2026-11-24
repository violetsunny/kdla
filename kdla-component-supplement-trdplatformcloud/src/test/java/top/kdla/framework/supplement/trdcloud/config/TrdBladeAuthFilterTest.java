package top.kdla.framework.supplement.trdcloud.config;

import cn.hutool.jwt.JWTUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import top.kdla.framework.domain.ApplicationContextHelp;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrdBladeAuthFilterTest {

    private static final String KEY = "unit-test-sign-key-0123456789abcdef";

    private final TrdBladeAuthFilter filter = new TrdBladeAuthFilter();
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("kdla.trd.jwt.sign-key", KEY);
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getEnvironment()).thenReturn(env);
        new ApplicationContextHelp().setApplicationContext(ctx);
        request = new MockHttpServletRequest("GET", "/trd/platform/detail/1");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        new ApplicationContextHelp().setApplicationContext(null);
    }

    private String token(long expEpochSeconds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("account", "zhangsan");
        payload.put("exp", expEpochSeconds);
        return JWTUtil.createToken(payload, KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validTokenPasses() throws Exception {
        request.addHeader("blade-auth", "Bearer " + token(System.currentTimeMillis() / 1000 + 3600));
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void missingHeaderRejected() throws Exception {
        // B-04：匿名请求不得到达控制器
        filter.doFilter(request, response, chain);
        verifyNoInteractions(chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false");
    }

    @Test
    void tamperedTokenRejected() throws Exception {
        String jwt = token(System.currentTimeMillis() / 1000 + 3600);
        String[] parts = jwt.split("\\.");
        char flipped = parts[2].charAt(0) == 'A' ? 'B' : 'A';
        request.addHeader("blade-auth", "Bearer " + parts[0] + "." + parts[1] + "." + flipped + parts[2].substring(1));
        filter.doFilter(request, response, chain);
        verifyNoInteractions(chain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void expiredTokenRejected() throws Exception {
        request.addHeader("blade-auth", "Bearer " + token(System.currentTimeMillis() / 1000 - 3600));
        filter.doFilter(request, response, chain);
        verifyNoInteractions(chain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void missingSignKeyConfigRejected() throws Exception {
        // fail-closed：密钥未配置时即使 token 格式合法也拒绝
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getEnvironment()).thenReturn(new MockEnvironment());
        new ApplicationContextHelp().setApplicationContext(ctx);
        request.addHeader("blade-auth", "Bearer " + token(System.currentTimeMillis() / 1000 + 3600));
        filter.doFilter(request, response, chain);
        verifyNoInteractions(chain);
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
