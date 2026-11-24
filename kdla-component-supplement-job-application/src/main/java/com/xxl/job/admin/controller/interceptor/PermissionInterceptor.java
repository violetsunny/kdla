package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.admin.controller.JobInfoController;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 权限拦截
 *
 * @author xuxueli 2015-12-12 18:09:04
 */
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {

	@Resource
	private LoginService loginService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		
		if (!(handler instanceof HandlerMethod)) {
			return true;	// proceed with the next interceptor
		}

		// if need login
		boolean needLogin = true;
		boolean needAdminuser = false;
		HandlerMethod method = (HandlerMethod)handler;
		PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
		if (permission!=null) {
			needLogin = permission.limit();
			needAdminuser = permission.adminuser();
		}

		if (needLogin) {
			XxlJobUser loginUser = loginService.ifLogin(request, response);
			if (loginUser == null && !needAdminuser && method.getBeanType() == JobInfoController.class) {
				// 机器调用（执行器/管理端 RPC）：XXL-JOB-ACCESS-TOKEN 鉴权，仅限 jobinfo 接口，合成管理员身份
				XxlJobAdminConfig adminConfig = XxlJobAdminConfig.getAdminConfig();
				String configuredToken = adminConfig != null ? adminConfig.getAccessToken() : null;
				String token = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);
				if (XxlJobRemotingUtil.validAccessToken(configuredToken, token)) {
					loginUser = new XxlJobUser();
					loginUser.setUsername("xxl-job-machine");
					loginUser.setRole(1);
				}
			}
			if (loginUser == null) {
				response.setStatus(302);
				response.setHeader("location", request.getContextPath()+"/toLogin");
				return false;
			}
			if (needAdminuser && loginUser.getRole()!=1) {
				throw new RuntimeException(I18nUtil.getString("system_permission_limit"));
			}
			request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
		}

		return true;	// proceed with the next interceptor
	}
}
