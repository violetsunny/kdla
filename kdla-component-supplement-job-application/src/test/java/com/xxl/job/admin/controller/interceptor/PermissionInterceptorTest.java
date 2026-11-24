package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.controller.JobCodeController;
import com.xxl.job.admin.controller.JobGroupController;
import com.xxl.job.admin.controller.JobInfoController;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionInterceptorTest {

    private PermissionInterceptor interceptor;

    @BeforeEach
    void setup() {
        interceptor = new PermissionInterceptor();
        LoginService loginService = mock(LoginService.class);
        when(loginService.ifLogin(any(), any())).thenReturn(null);
        ReflectionTestUtils.setField(interceptor, "loginService", loginService);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", null);
    }

    private void adminToken(String token) {
        XxlJobAdminConfig cfg = new XxlJobAdminConfig();
        ReflectionTestUtils.setField(cfg, "accessToken", token);
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", cfg);
    }

    private HandlerMethod jobInfoHandler() throws Exception {
        Method m = JobInfoController.class.getMethod("start", HttpServletRequest.class, int.class);
        return new HandlerMethod(new JobInfoController(), m);
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setContextPath("/xxl-job-admin");
        if (token != null) {
            req.addHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN, token);
        }
        return req;
    }

    @Test
    void jobInfoEndpointWithoutAuthRedirectsToLogin() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(requestWithToken(null), resp, jobInfoHandler());
        assertThat(proceed).isFalse();
        assertThat(resp.getStatus()).isEqualTo(302);
    }

    @Test
    void validMachineTokenAllowsJobInfo() throws Exception {
        adminToken("secret");
        MockHttpServletRequest req = requestWithToken("secret");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(req, resp, jobInfoHandler());
        assertThat(proceed).isTrue();
        XxlJobUser u = (XxlJobUser) req.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        assertThat(u).isNotNull();
        assertThat(u.getRole()).isEqualTo(1);
    }

    @Test
    void emptyConfiguredTokenRejectsMachine() throws Exception {
        adminToken("");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(requestWithToken("secret"), resp, jobInfoHandler());
        assertThat(proceed).isFalse();
        assertThat(resp.getStatus()).isEqualTo(302);
    }

    @Test
    void wrongMachineTokenRejects() throws Exception {
        adminToken("secret");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(requestWithToken("wrong"), resp, jobInfoHandler());
        assertThat(proceed).isFalse();
        assertThat(resp.getStatus()).isEqualTo(302);
    }

    @Test
    void adminuserEndpointRejectsMachineToken() throws Exception {
        adminToken("secret");
        Method m = JobGroupController.class.getMethod("remove", int.class);
        HandlerMethod handler = new HandlerMethod(new JobGroupController(), m);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(requestWithToken("secret"), resp, handler);
        assertThat(proceed).isFalse();
        assertThat(resp.getStatus()).isEqualTo(302);
    }

    @Test
    void nonJobInfoEndpointRejectsMachineToken() throws Exception {
        adminToken("secret");
        Method m = JobCodeController.class.getMethod("save", HttpServletRequest.class, Model.class, int.class, String.class, String.class);
        HandlerMethod handler = new HandlerMethod(new JobCodeController(), m);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean proceed = interceptor.preHandle(requestWithToken("secret"), resp, handler);
        assertThat(proceed).isFalse();
        assertThat(resp.getStatus()).isEqualTo(302);
    }
}
