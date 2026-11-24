package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.admin.service.LoginService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * CSRF 前端接线回归：token 须经公共宏注入页面、经公共 $.ajaxSetup 回传，否则服务端校验会拦掉全部正常请求。
 * 9 个页面均 import common.macro.ftl，26 个 POST 调用点均走 jQuery.ajax，故只需守住这两个单点。
 */
class CsrfTokenFrontendWiringTest {

    private LoginService loginService;
    private CsrfTokenAdvice advice;

    @BeforeEach
    void setup() {
        loginService = new LoginService();
        ReflectionTestUtils.setField(loginService, "hmacKey",
                "test-secret-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(loginService, "xxlJobUserDao", mock(XxlJobUserDao.class));
        advice = new CsrfTokenAdvice();
        ReflectionTestUtils.setField(advice, "loginService", loginService);
    }

    @Test
    void adviceReturnsEmptyWithoutCookie() {
        assertThat(advice.csrfToken(new MockHttpServletRequest())).isEmpty();
    }

    @Test
    void adviceReturnsSessionBoundTokenWithCookie() {
        XxlJobUser u = new XxlJobUser();
        u.setUsername("admin");
        u.setPassword("hash123");
        u.setRole(1);
        String token = ReflectionTestUtils.invokeMethod(loginService, "makeToken", u);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie(LoginService.LOGIN_IDENTITY_KEY, token));
        assertThat(advice.csrfToken(req)).isEqualTo(loginService.makeCsrfToken(token));
    }

    @Test
    void publicMacroInjectsTokenVariable() throws Exception {
        String ftl = resource("templates/common/common.macro.ftl");
        assertThat(ftl).contains("var csrf_token = '${csrfToken!''}';");
    }

    @Test
    void commonScriptSetsHeaderOnEveryAjax() throws Exception {
        String js = resource("static/js/common.1.js");
        assertThat(js).contains("$.ajaxSetup({");
        assertThat(js).contains("xhr.setRequestHeader('" + LoginService.CSRF_HEADER + "', csrf_token)");
    }

    @Test
    void publicMacroTemplateParses() throws Exception {
        freemarker.template.Configuration cfg =
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_0);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        assertThat(cfg.getTemplate("common/common.macro.ftl")).isNotNull();
    }

    private static String resource(String path) throws Exception {
        try (InputStream in = CsrfTokenFrontendWiringTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
