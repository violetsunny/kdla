package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.controller.IndexController;
import com.xxl.job.admin.controller.JobInfoController;
import com.xxl.job.admin.controller.UserController;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * B-09 回归测试：变更请求缺逐请求 CSRF token 校验，仅靠 SameSite=Lax 单层缓解。
 *
 * <p>旧实现：16 个变更端点限 POST + Cookie SameSite=Lax，无 token 校验；
 * 一旦 SameSite 失效（旧浏览器/子域攻击）即可跨站发起变更。
 *
 * <p>修复后：会话绑定 token = hex(HMAC("csrf|" + 会话token))，浏览器变更请求须带 X-CSRF-TOKEN 头。
 * 豁免：安全方法(GET/HEAD/OPTIONS/TRACE)、无会话 cookie 的匿名请求(login)、accessToken 鉴权通过的机器调用。
 *
 * <p>无修复则失败：旧代码带 cookie 的 POST 无 token 头一律放行（proceed=true，无 403）。
 */
class CsrfTokenProtectionTest {

    private static final byte[] KEY = "test-secret-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    private LoginService loginService;
    private PermissionInterceptor interceptor;
    private String cookieToken;

    @BeforeEach
    void setup() {
        loginService = new LoginService();
        ReflectionTestUtils.setField(loginService, "hmacKey", KEY);
        XxlJobUserDao dao = mock(XxlJobUserDao.class);
        XxlJobUser u = new XxlJobUser();
        u.setUsername("admin");
        u.setPassword("hash123");
        u.setRole(1);
        when(dao.loadByUserName("admin")).thenReturn(u);
        ReflectionTestUtils.setField(loginService, "xxlJobUserDao", dao);
        cookieToken = ReflectionTestUtils.invokeMethod(loginService, "makeToken", u);

        interceptor = new PermissionInterceptor();
        ReflectionTestUtils.setField(interceptor, "loginService", loginService);

        XxlJobAdminConfig cfg = new XxlJobAdminConfig();
        ReflectionTestUtils.setField(cfg, "i18n", "zh_CN");
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", cfg);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", null);
    }

    private HandlerMethod handler(Class<?> controller, String name) throws Exception {
        for (Method m : controller.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return new HandlerMethod(controller.getDeclaredConstructor().newInstance(), m);
            }
        }
        throw new AssertionError("未找到方法:" + controller.getSimpleName() + "#" + name);
    }

    private MockHttpServletRequest request(String method, boolean withCookie) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setContextPath("/xxl-job-admin");
        if (withCookie) {
            req.setCookies(new Cookie(LoginService.LOGIN_IDENTITY_KEY, cookieToken));
        }
        return req;
    }

    private void adminToken(String token) {
        XxlJobAdminConfig cfg = new XxlJobAdminConfig();
        ReflectionTestUtils.setField(cfg, "accessToken", token);
        ReflectionTestUtils.setField(cfg, "i18n", "zh_CN");
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", cfg);
    }

    @Test
    void postWithCookieButNoCsrfHeaderRejected() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request("POST", true), resp, handler(UserController.class, "add"))).isFalse();
        assertThat(resp.getStatus()).isEqualTo(403);
    }

    @Test
    void postWithValidCsrfHeaderAllowed() throws Exception {
        MockHttpServletRequest req = request("POST", true);
        req.addHeader(LoginService.CSRF_HEADER, loginService.makeCsrfToken(cookieToken));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(req, resp, handler(UserController.class, "add"))).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void postWithForgedCsrfHeaderRejected() throws Exception {
        MockHttpServletRequest req = request("POST", true);
        req.addHeader(LoginService.CSRF_HEADER, "deadbeef");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(req, resp, handler(UserController.class, "add"))).isFalse();
        assertThat(resp.getStatus()).isEqualTo(403);
    }

    @Test
    void csrfTokenOfAnotherSessionRejected() throws Exception {
        XxlJobUser other = new XxlJobUser();
        other.setUsername("other");
        other.setPassword("hash456");
        other.setRole(1);
        String otherToken = ReflectionTestUtils.invokeMethod(loginService, "makeToken", other);
        MockHttpServletRequest req = request("POST", true);
        req.addHeader(LoginService.CSRF_HEADER, loginService.makeCsrfToken(otherToken));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(req, resp, handler(UserController.class, "add"))).isFalse();
        assertThat(resp.getStatus()).isEqualTo(403);
    }

    @Test
    void safeMethodNotCsrfChecked() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request("GET", true), resp, handler(UserController.class, "add"))).isTrue();
    }

    @Test
    void machineTokenPostExempt() throws Exception {
        adminToken("secret");
        MockHttpServletRequest req = request("POST", false);
        req.addHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN, "secret");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(req, resp, handler(JobInfoController.class, "start"))).isTrue();
    }

    @Test
    void bogusMachineTokenDoesNotBypassCsrf() throws Exception {
        adminToken("secret");
        MockHttpServletRequest req = request("POST", true);
        req.addHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN, "wrong");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(req, resp, handler(JobInfoController.class, "start"))).isFalse();
        assertThat(resp.getStatus()).isEqualTo(403);
    }

    @Test
    void anonymousPostWithoutCookieExempt() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request("POST", false), resp, handler(IndexController.class, "loginDo"))).isTrue();
    }

    @Test
    void logoutWithCookieRequiresCsrf() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request("POST", true), resp, handler(IndexController.class, "logout"))).isFalse();
        assertThat(resp.getStatus()).isEqualTo(403);
    }

    @Test
    void csrfTokenDeterministicBoundAndDomainSeparated() {
        String csrf = loginService.makeCsrfToken(cookieToken);
        assertThat(loginService.makeCsrfToken(cookieToken)).isEqualTo(csrf);
        assertThat(csrf).isNotEqualTo(cookieToken);
        assertThat(loginService.validCsrfToken(cookieToken, csrf)).isTrue();
        assertThat(loginService.validCsrfToken(cookieToken, null)).isFalse();
        assertThat(loginService.validCsrfToken(cookieToken, "")).isFalse();
        assertThat(loginService.validCsrfToken(null, csrf)).isFalse();
    }
}
