package com.xxl.job.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class CsrfMethodRestrictionTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new JobInfoController(), new JobCodeController(), new JobGroupController(),
            new JobLogController(), new UserController()).build();

    @Test
    void stateChangingEndpointsRejectGet() throws Exception {
        String[] paths = {
                "/jobinfo/add", "/jobinfo/update", "/jobinfo/remove",
                "/jobinfo/stop", "/jobinfo/start", "/jobinfo/trigger",
                "/jobcode/save",
                "/jobgroup/save", "/jobgroup/update", "/jobgroup/remove",
                "/joblog/logKill", "/joblog/clearLog",
                "/user/add", "/user/update", "/user/remove", "/user/updatePwd"
        };
        for (String path : paths) {
            int status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isEqualTo(405);
        }
    }
}
