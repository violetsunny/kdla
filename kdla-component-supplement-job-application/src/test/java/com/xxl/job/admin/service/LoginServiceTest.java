package com.xxl.job.admin.service;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.JacksonUtil;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.core.biz.model.ReturnT;
import top.kdla.framework.common.utils.BCryptUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private static final byte[] KEY = "test-secret-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    private LoginService loginService;
    private XxlJobUserDao dao;

    @BeforeEach
    void setup() {
        loginService = new LoginService();
        ReflectionTestUtils.setField(loginService, "hmacKey", KEY);
        dao = mock(XxlJobUserDao.class);
        ReflectionTestUtils.setField(loginService, "xxlJobUserDao", dao);
        XxlJobAdminConfig cfg = new XxlJobAdminConfig();
        ReflectionTestUtils.setField(cfg, "i18n", "zh_CN");
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", cfg);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", null);
    }

    private XxlJobUser user() {
        XxlJobUser u = new XxlJobUser();
        u.setUsername("admin");
        u.setPassword("hash123");
        u.setRole(1);
        return u;
    }

    private String tokenWithExp(XxlJobUser u, long exp) throws Exception {
        String payload = JacksonUtil.writeValueAsString(u) + "|" + exp;
        byte[] pb = payload.getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
        return HexFormat.of().formatHex(pb) + "." + HexFormat.of().formatHex(mac.doFinal(pb));
    }

    private XxlJobUser ifLogin(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie(LoginService.LOGIN_IDENTITY_KEY, token));
        return loginService.ifLogin(req, new MockHttpServletResponse());
    }

    @Test
    void forgedUnsignedTokenRejected() {
        String forged = HexFormat.of().formatHex(JacksonUtil.writeValueAsString(user()).getBytes(StandardCharsets.UTF_8));
        assertThat(ifLogin(forged)).isNull();
    }

    @Test
    void validSignedTokenAccepted() {
        XxlJobUser u = user();
        when(dao.loadByUserName("admin")).thenReturn(u);
        String token = ReflectionTestUtils.invokeMethod(loginService, "makeToken", u);
        assertThat(ifLogin(token)).isNotNull();
    }

    @Test
    void expiredTokenRejected() throws Exception {
        String token = tokenWithExp(user(), System.currentTimeMillis() - 1000L);
        assertThat(ifLogin(token)).isNull();
    }

    @Test
    void tamperedSignatureRejected() {
        String token = ReflectionTestUtils.invokeMethod(loginService, "makeToken", user());
        char last = token.charAt(token.length() - 1);
        char flipped = last == '0' ? '1' : '0';
        String tampered = token.substring(0, token.length() - 1) + flipped;
        assertThat(ifLogin(tampered)).isNull();
    }

    private ReturnT<String> login(String username, String password) {
        return loginService.login(new MockHttpServletRequest(), new MockHttpServletResponse(), username, password, false);
    }

    @Test
    void bcryptPasswordAccepted() {
        XxlJobUser u = user();
        u.setPassword(BCryptUtil.hashPassword("Secret123"));
        when(dao.loadByUserName("admin")).thenReturn(u);
        assertThat(login("admin", "Secret123").getCode()).isEqualTo(200);
        verify(dao, never()).update(any());
    }

    @Test
    void legacyMd5MigratedToBcrypt() {
        XxlJobUser u = user();
        u.setPassword(DigestUtils.md5DigestAsHex("Secret123".getBytes(StandardCharsets.UTF_8)));
        when(dao.loadByUserName("admin")).thenReturn(u);
        assertThat(login("admin", "Secret123").getCode()).isEqualTo(200);
        verify(dao).update(argThat(x -> x.getPassword().startsWith("$2")));
    }

    @Test
    void wrongPasswordRejected() {
        XxlJobUser u = user();
        u.setPassword(BCryptUtil.hashPassword("Secret123"));
        when(dao.loadByUserName("admin")).thenReturn(u);
        assertThat(login("admin", "wrong").getCode()).isEqualTo(500);
    }

    @Test
    void lockoutAfterRepeatedFailures() {
        XxlJobUser u = user();
        u.setPassword(BCryptUtil.hashPassword("Secret123"));
        when(dao.loadByUserName("admin")).thenReturn(u);
        for (int i = 0; i < 5; i++) {
            assertThat(login("admin", "wrong").getCode()).isEqualTo(500);
        }
        assertThat(login("admin", "Secret123").getCode()).isEqualTo(500);
    }
}
