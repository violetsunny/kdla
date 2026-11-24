package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.util.CookieUtil;
import com.xxl.job.admin.service.LoginService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 向所有页面注入会话绑定 CSRF token，由 common.macro.ftl 输出给前端 $.ajaxSetup 统一回传
 */
@ControllerAdvice
public class CsrfTokenAdvice {

    @Resource
    private LoginService loginService;

    @ModelAttribute("csrfToken")
    public String csrfToken(HttpServletRequest request) {
        String cookieToken = CookieUtil.getValue(request, LoginService.LOGIN_IDENTITY_KEY);
        return cookieToken == null ? "" : loginService.makeCsrfToken(cookieToken);
    }
}
