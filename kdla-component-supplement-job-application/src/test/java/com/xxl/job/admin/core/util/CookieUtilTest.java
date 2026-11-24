package com.xxl.job.admin.core.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    @Test
    void rememberCookieHasSecurityAttributes() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSecure(true);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        CookieUtil.set(req, resp, "k", "v", true);
        String header = resp.getHeader("Set-Cookie");
        assertThat(header).contains("SameSite=Lax").contains("HttpOnly").contains("Secure").contains("Max-Age=7200").contains("Path=/");
    }

    @Test
    void sessionCookieOmitsMaxAge() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        CookieUtil.set(req, resp, "k", "v", false);
        assertThat(resp.getHeader("Set-Cookie")).doesNotContain("Max-Age");
    }

    @Test
    void httpCookieNotSecure() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSecure(false);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        CookieUtil.set(req, resp, "k", "v", true);
        assertThat(resp.getHeader("Set-Cookie")).doesNotContain("Secure");
    }

    @Test
    void removeExpiresCookie() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("k", "v"));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        CookieUtil.remove(req, resp, "k");
        assertThat(resp.getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
