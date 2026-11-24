package top.kdla.framework.supplement.trdcloud.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import top.kdla.framework.domain.ApplicationContextHelp;
import top.kdla.framework.exception.BizException;

import java.nio.charset.StandardCharsets;

/**
 * JWT 工具：强制 Bearer 格式、HS256 验签、exp 声明必须存在且未过期，任何失败抛 BizException，绝不降级放行。
 * 破坏性变更：下游必须配置 kdla.trd.jwt.sign-key（UTF-8 字节长度≥32），未配置或验签失败时依赖本类的 trd 接口立即不可用。
 * 密钥编码假设：raw UTF-8 bytes，未对真实 BladeX token 实测。
 */
@Slf4j
public class JwtUtil {

    private static final String SIGN_KEY_PROPERTY = "kdla.trd.jwt.sign-key";

    /**
     * 获取 token 中的属性，验签与过期校验通过后才返回
     *
     * @param auth blade-auth 头内容，格式 "Bearer <token>"
     * @param prop 要获取的属性名
     * @return 属性值，属性缺失返回 null
     */
    public static String getInfoFromToken(String auth, String prop) {
        String signKey = requireSignKey();
        String token = extractToken(auth);
        JWT jwt;
        try {
            jwt = JWTUtil.parseToken(token)
                    .setSigner(JWTSignerUtil.hs256(signKey.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.warn("JWT 解析失败");
            throw new BizException("JWT 格式非法");
        }
        if (!jwt.verify()) {
            log.warn("JWT 验签失败");
            throw new BizException("JWT 验签失败");
        }
        JSONObject payloads = jwt.getPayloads();
        if (payloads == null || payloads.get("exp") == null) {
            log.warn("JWT 缺少 exp 声明");
            throw new BizException("JWT 缺少 exp 声明");
        }
        if (!jwt.validate(0)) {
            log.warn("JWT 过期");
            throw new BizException("JWT 已过期或时间声明非法");
        }
        return payloads.getStr(prop);
    }

    /**
     * fail-closed：容器未注入或密钥未配置/空白一律抛异常
     */
    private static String requireSignKey() {
        ApplicationContext context = ApplicationContextHelp.getApplicationContext();
        if (context == null) {
            throw new BizException("ApplicationContext 未初始化，无法读取 JWT 签名密钥");
        }
        String signKey = context.getEnvironment().getProperty(SIGN_KEY_PROPERTY);
        if (signKey == null || signKey.isBlank()) {
            throw new BizException("未配置 " + SIGN_KEY_PROPERTY + "，JWT 验签不可用");
        }
        if (signKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BizException(SIGN_KEY_PROPERTY + " 密钥长度不足 32 字节，不满足 HS256 最小强度");
        }
        return signKey;
    }

    private static String extractToken(String auth) {
        if (auth == null) {
            throw new BizException("auth 头为空");
        }
        String[] parts = auth.split(" ");
        if (parts.length < 2 || parts[1].isEmpty() || !"Bearer".equalsIgnoreCase(parts[0])) {
            throw new BizException("auth 头格式非法，应为 Bearer <token>");
        }
        return parts[1];
    }

}