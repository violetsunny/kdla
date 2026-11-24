package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.core.biz.model.ReturnT;
import top.kdla.framework.common.utils.BCryptUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserController controller;
    private XxlJobUserDao userDao;

    @BeforeEach
    void setup() {
        controller = new UserController();
        userDao = mock(XxlJobUserDao.class);
        ReflectionTestUtils.setField(controller, "xxlJobUserDao", userDao);
        ReflectionTestUtils.setField(controller, "xxlJobGroupDao", mock(XxlJobGroupDao.class));
        XxlJobAdminConfig cfg = new XxlJobAdminConfig();
        ReflectionTestUtils.setField(cfg, "i18n", "zh_CN");
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", cfg);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(XxlJobAdminConfig.class, "adminConfig", null);
    }

    private MockHttpServletRequest requestAs(String username) {
        XxlJobUser loginUser = new XxlJobUser();
        loginUser.setUsername(username);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
        return req;
    }

    private XxlJobUser storedUser(String rawPassword) {
        XxlJobUser u = new XxlJobUser();
        u.setUsername("admin");
        u.setPassword(BCryptUtil.hashPassword(rawPassword));
        return u;
    }

    @Test
    void updatePwdRejectsWrongOldPassword() {
        when(userDao.loadByUserName("admin")).thenReturn(storedUser("OldSecret1"));
        ReturnT<String> r = controller.updatePwd(requestAs("admin"), "wrongOld", "NewSecret123");
        assertThat(r.getCode()).isEqualTo(500);
        verify(userDao, never()).update(any());
    }

    @Test
    void updatePwdAcceptsCorrectOldPassword() {
        when(userDao.loadByUserName("admin")).thenReturn(storedUser("OldSecret1"));
        ReturnT<String> r = controller.updatePwd(requestAs("admin"), "OldSecret1", "NewSecret123");
        assertThat(r.getCode()).isEqualTo(200);
        verify(userDao).update(argThat(u -> u.getPassword().startsWith("$2")));
    }

    @Test
    void updatePwdRejectsMissingOldPassword() {
        ReturnT<String> r = controller.updatePwd(requestAs("admin"), null, "NewSecret123");
        assertThat(r.getCode()).isEqualTo(500);
        verify(userDao, never()).update(any());
    }

    @Test
    void addRejectsShortPassword() {
        XxlJobUser u = new XxlJobUser();
        u.setUsername("testuser");
        u.setPassword("abc");
        ReturnT<String> r = controller.add(u);
        assertThat(r.getCode()).isEqualTo(500);
        verify(userDao, never()).save(any());
    }
}
