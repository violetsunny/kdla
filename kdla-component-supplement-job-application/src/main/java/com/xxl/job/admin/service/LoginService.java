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
import top.kdla.framework.common.utils.BCryptUtil;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xuxueli 2019-05-04 22:13:264
 */
@Configuration
public class LoginService {

    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    public static final String CSRF_HEADER = "X-CSRF-TOKEN";

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    // 会话 token 有效期 2H，与 Cookie maxAge 对齐
    private static final long TOKEN_TTL_MILLIS = 2 * 60 * 60 * 1000L;

    // 登录失败限速：5 次失败锁定 15 分钟
    // ponytail: 单实例内存计数，重启清零、多实例不共享；升级路径=Redis 集中计数
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOGIN_LOCK_MILLIS = 15 * 60 * 1000L;
    private final ConcurrentHashMap<String, long[]> loginFailures = new ConcurrentHashMap<>();

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

    /**
     * 会话绑定 CSRF token：hex(HMAC("csrf|" + 会话token))。域分隔前缀避免与会话签名混用。
     * ponytail: 会话期内固定，非一次性；一次性 token 需服务端存 nonce，与现有无状态会话冲突
     */
    public String makeCsrfToken(String cookieToken) {
        return HexFormat.of().formatHex(hmac(("csrf|" + cookieToken).getBytes(StandardCharsets.UTF_8)));
    }

    public boolean validCsrfToken(String cookieToken, String presented) {
        if (cookieToken == null || presented == null || presented.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(makeCsrfToken(cookieToken).getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * 口令校验：BCrypt（$2 前缀）优先，兼容旧 MD5 十六进制。登录与改密共用此唯一路径。
     */
    public static boolean matchesPassword(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (stored.startsWith("$2")) {
            return BCryptUtil.checkPassword(raw, stored);
        }
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)).equals(stored);
    }

    private boolean isLoginLocked(String username) {
        long[] rec = loginFailures.get(username);
        return rec != null && System.currentTimeMillis() < rec[1];
    }

    private void recordLoginFailure(String username) {
        loginFailures.compute(username, (k, rec) -> {
            long now = System.currentTimeMillis();
            if (rec == null || (rec[1] > 0 && now >= rec[1])) {
                rec = new long[]{0, 0};
            }
            rec[0]++;
            if (rec[0] >= MAX_LOGIN_FAILURES) {
                rec[1] = now + LOGIN_LOCK_MILLIS;
            }
            return rec;
        });
    }

    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember){

        // param
        if (username==null || username.trim().length()==0 || password==null || password.trim().length()==0){
            return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
        }

        // 失败限速
        if (isLoginLocked(username)) {
            return new ReturnT<String>(500, "登录失败次数过多，请稍后再试");
        }

        // valid password（BCrypt，兼容旧 MD5）
        XxlJobUser xxlJobUser = xxlJobUserDao.loadByUserName(username);
        if (xxlJobUser == null || !matchesPassword(password, xxlJobUser.getPassword())) {
            recordLoginFailure(username);
            return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
        }

        // 旧 MD5 口令懒迁移到 BCrypt
        String stored = xxlJobUser.getPassword();
        if (stored != null && !stored.startsWith("$2")) {
            xxlJobUser.setPassword(BCryptUtil.hashPassword(password));
            xxlJobUserDao.update(xxlJobUser);
        }
        loginFailures.remove(username);

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
