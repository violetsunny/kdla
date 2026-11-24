package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobCodeControllerTest {

    @Test
    void saveRejectsUserWithoutGroupPermission() {
        JobCodeController controller = new JobCodeController();
        XxlJobInfoDao dao = mock(XxlJobInfoDao.class);
        XxlJobInfo info = new XxlJobInfo();
        info.setJobGroup(5);
        when(dao.loadById(7)).thenReturn(info);
        ReflectionTestUtils.setField(controller, "xxlJobInfoDao", dao);

        MockHttpServletRequest request = new MockHttpServletRequest();
        XxlJobUser user = new XxlJobUser();
        user.setRole(0);
        user.setPermission("1,2");
        user.setUsername("bob");
        request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, user);

        assertThatThrownBy(() -> controller.save(request, null, 7, "code", "remark"))
                .isInstanceOf(RuntimeException.class);
        verify(dao, never()).update(any());
    }
}
