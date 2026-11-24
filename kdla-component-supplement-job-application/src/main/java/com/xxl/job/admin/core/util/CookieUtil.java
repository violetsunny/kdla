package com.xxl.job.admin.core.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * Cookie.Util
 *
 * @author xuxueli 2015-12-12 18:01:06
 */
public class CookieUtil {

	// 缓存时间 2H，与会话 token 过期对齐
	private static final int COOKIE_MAX_AGE = 2 * 60 * 60;
	// 保存路径,根路径
	private static final String COOKIE_PATH = "/";

	/**
	 * 保存：HttpOnly + SameSite=Lax（CSRF 缓解）+ Secure（HTTPS 时）
	 */
	public static void set(HttpServletRequest request, HttpServletResponse response, String key, String value, boolean ifRemember) {
		int age = ifRemember ? COOKIE_MAX_AGE : -1;
		addCookie(request, response, key, value, age);
	}

	private static void addCookie(HttpServletRequest request, HttpServletResponse response, String key, String value, int maxAge) {
		ResponseCookie.ResponseCookieBuilder spec = ResponseCookie.from(key, value)
				.path(COOKIE_PATH)
				.httpOnly(true)
				.secure(request != null && request.isSecure())
				.sameSite("Lax");
		if (maxAge >= 0) {
			spec.maxAge(maxAge);
		}
		response.addHeader(HttpHeaders.SET_COOKIE, spec.build().toString());
	}

	/**
	 * 查询value
	 */
	public static String getValue(HttpServletRequest request, String key) {
		Cookie cookie = get(request, key);
		if (cookie != null) {
			return cookie.getValue();
		}
		return null;
	}

	private static Cookie get(HttpServletRequest request, String key) {
		Cookie[] arr_cookie = request.getCookies();
		if (arr_cookie != null && arr_cookie.length > 0) {
			for (Cookie cookie : arr_cookie) {
				if (cookie.getName().equals(key)) {
					return cookie;
				}
			}
		}
		return null;
	}

	/**
	 * 删除Cookie
	 */
	public static void remove(HttpServletRequest request, HttpServletResponse response, String key) {
		Cookie cookie = get(request, key);
		if (cookie != null) {
			addCookie(request, response, key, "", 0);
		}
	}

}
