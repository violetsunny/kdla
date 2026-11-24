package com.xxl.job.admin.service;

import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.JacksonUtil;
import com.xxl.job.admin.dao.XxlJobUserDao;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
}
