package top.kdla.framework.log.sanitize;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 日志脱敏与截断工具。
 * <p>
 * 内部用途：仅供 kdla-component-log 模块的拦截器/切面在打印请求体、Header、响应时调用，
 * 业务代码不应直接依赖本类（后续可能随日志组件调整而不另行通知）。
 * <p>
 * 规则：
 * <ul>
 *     <li>Header 名命中黑名单（authorization/cookie/password/token/mobile/blade-auth，忽略大小写）时值替换为 ***</li>
 *     <li>日志体超过 2KB（2048 字符）截断，并追加 "...(truncated, total=原始长度)"</li>
 * </ul>
 *
 * @author kll
 */
public final class LogSanitizeUtil {

    /**
     * 敏感 Header 名黑名单（统一小写比较）
     */
    private static final Set<String> SENSITIVE_HEADER_NAMES = new HashSet<>(Arrays.asList(
            "authorization", "cookie", "password", "token", "mobile", "blade-auth"));

    /**
     * 脱敏替换值
     */
    private static final String MASK = "***";

    /**
     * 日志体最大长度：2KB（按字符数计）
     */
    private static final int MAX_BODY_LENGTH = 2048;

    private LogSanitizeUtil() {
    }

    /**
     * Header 值脱敏：名称命中黑名单（忽略大小写）返回 ***，否则原样返回
     *
     * @param headerName  Header 名，可为 null
     * @param headerValue Header 原始值
     * @return 脱敏后的值
     */
    public static String maskHeader(String headerName, String headerValue) {
        if (headerName != null && SENSITIVE_HEADER_NAMES.contains(headerName.toLowerCase(Locale.ROOT))) {
            return MASK;
        }
        return headerValue;
    }

    /**
     * 日志体截断：超过 2KB 截断并追加原始长度标记，null 与未超长原样返回
     *
     * @param body 日志体
     * @return 截断后的日志体
     */
    public static String truncate(String body) {
        if (body == null || body.length() <= MAX_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LENGTH) + "...(truncated, total=" + body.length() + ")";
    }
}
