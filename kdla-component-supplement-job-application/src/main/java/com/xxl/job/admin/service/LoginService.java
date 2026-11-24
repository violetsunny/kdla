package com.xxl.job.admin.service;

import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.CookieUtil;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.core.util.JacksonUtil;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * @author xuxueli 2019-05-04 22:13:264
 */
@Configuration
public class LoginService {

    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    // 会话 token 有效期 2H，与 Cookie maxAge 对齐
    private static final long TOKEN_TTL_MILLIS = 2 * 60 * 60 * 1000L;

    @Value("${xxl.job.login.tokenSecret:}")
    private String tokenSecret;

    private byte[] hmacKey;

    @PostConstruct
    void initHmacKey() {
        if (tokenSecret != null && !tokenSecret.trim().isEmpty()) {
            hmacKey = tokenSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            // 未配置密钥时随机生成：重启后会话失效，多实例需配置共享 xxl.job.login.tokenSecret
            hmacKey = new byte[32];
            new SecureRandom().nextBytes(hmacKey);
        }
    }


    private String makeToken(XxlJobUser xxlJobUser){
        long exp = System.currentTimeMillis() + TOKEN_TTL_MILLIS;
        String payload = JacksonUtil.writeValueAsString(xxlJobUser) + "|" + exp;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(payloadBytes) + "." + HexFormat.of().formatHex(hmac(payloadBytes));
    }
    private XxlJobUser parseToken(String token){
        if (token == null) {
            return null;
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return null;
        }
        byte[] payloadBytes;
        byte[] sig;
        try {
            payloadBytes = HexFormat.of().parseHex(token.substring(0, dot));
            sig = HexFormat.of().parseHex(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!MessageDigest.isEqual(sig, hmac(payloadBytes))) {
            return null;
        }
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        int sep = payload.lastIndexOf('|');
        if (sep < 0) {
            return null;
        }
        long exp;
        try {
            exp = Long.parseLong(payload.substring(sep + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (System.currentTimeMillis() > exp) {
            return null;
        }
        return JacksonUtil.readValue(payload.substring(0, sep), XxlJobUser.class);
    }
    private byte[] hmac(byte[] data){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("会话 token HMAC 计算失败", e);
        }
    }


    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember){

        // param
        if (username==null || username.trim().length()==0 || password==null || password.trim().length()==0){
            return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
        }

        // valid passowrd
        XxlJobUser xxlJobUser = xxlJobUserDao.loadByUserName(username);
        if (xxlJobUser == null) {
            return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!passwordMd5.equals(xxlJobUser.getPassword())) {
            return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
        }

        String loginToken = makeToken(xxlJobUser);

        // do login
        CookieUtil.set(request, response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @param response
     */
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @return
     */
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response){
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (cookieToken != null) {
            XxlJobUser cookieUser = null;
            try {
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
            }
            if (cookieUser != null) {
                XxlJobUser dbUser = xxlJobUserDao.loadByUserName(cookieUser.getUsername());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }


}
