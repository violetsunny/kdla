package top.kdla.framework.log.sanitize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 日志脱敏与截断工具。
 * <p>
 * 内部用途：仅供 kdla-component-log 模块的拦截器/切面在打印请求体、Header、响应时调用，
 * 业务代码不应直接依赖本类（后续可能随日志组件调整而不另行通知）。
 * <p>
 * 规则：
 * <ul>
 *     <li>Header/JSON 键名命中敏感名（精确黑名单 + token/secret/credential/apikey/session/csrf/idcard 变体匹配，忽略大小写）时值替换为 ***</li>
 *     <li>日志体超过 2KB（2048 字符）截断，并追加 "...(truncated, total=原始长度)"</li>
 *     <li>外来 traceId 仅允许 [A-Za-z0-9-]{1,64}，防 CRLF 伪造日志行</li>
 * </ul>
 *
 * @author kll
 */
public final class LogSanitizeUtil {

    /**
     * 敏感名精确黑名单（统一小写比较），Header 与 JSON 字段共用
     */
    private static final Set<String> SENSITIVE_NAMES = new HashSet<>(Arrays.asList(
            "authorization", "cookie", "set-cookie", "proxy-authenticate", "proxy-authorization",
            "password", "pwd", "mobile", "phone", "blade-auth",
            "idcard", "idcardno", "certificatecode"));

    /**
     * 脱敏替换值
     */
    private static final String MASK = "***";

    /**
     * 日志体最大长度：2KB（按字符数计）
     */
    private static final int MAX_BODY_LENGTH = 2048;

    /**
     * 外来 traceId 格式白名单
     */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private LogSanitizeUtil() {
    }

    /**
     * Header 值脱敏：名称命中敏感名（忽略大小写）返回 ***，否则原样返回
     *
     * @param headerName  Header 名，可为 null
     * @param headerValue Header 原始值
     * @return 脱敏后的值
     */
    public static String maskHeader(String headerName, String headerValue) {
        if (isSensitive(headerName)) {
            return MASK;
        }
        return headerValue;
    }

    /**
     * JSON 字段级掩码：递归对敏感键的值打 ***；非 JSON 对象/数组（纯文本、标量）原样返回。
     *
     * @param json JSON 字符串，可为 null
     * @return 掩码后的 JSON
     */
    public static String maskJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Object parsed;
        try {
            parsed = JSON.parse(json);
        } catch (Exception e) {
            // 非合法 JSON（如表单/纯文本 body）原样返回，交由截断控制体积
            return json;
        }
        if (!(parsed instanceof JSONObject) && !(parsed instanceof JSONArray)) {
            return json;
        }
        maskNode(parsed);
        return JSON.toJSONString(parsed);
    }

    /**
     * 日志体统一净化：字段级掩码 + 2KB 截断
     *
     * @param body 日志体
     * @return 净化后的日志体
     */
    public static String sanitize(String body) {
        return truncate(maskJson(body));
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

    /**
     * 外来 traceId 格式校验：仅允许 [A-Za-z0-9-]{1,64}，不合法（含 CRLF/超长）须自生成
     *
     * @param traceId 外来 traceId，可为 null
     * @return 是否合法
     */
    public static boolean validTraceId(String traceId) {
        return traceId != null && TRACE_ID_PATTERN.matcher(traceId).matches();
    }

    private static void maskNode(Object node) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            for (String key : obj.keySet()) {
                if (isSensitive(key)) {
                    obj.put(key, MASK);
                } else {
                    maskNode(obj.get(key));
                }
            }
        } else if (node instanceof JSONArray) {
            for (Object item : (JSONArray) node) {
                maskNode(item);
            }
        }
    }

    private static boolean isSensitive(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (SENSITIVE_NAMES.contains(lower)) {
            return true;
        }
        // 变体匹配：覆盖 x-access-token/apiKey/sessionId 等自定义鉴权头与字段
        return lower.endsWith("token")
                || lower.contains("password")
                || lower.contains("secret")
                || lower.contains("credential")
                || lower.contains("apikey")
                || lower.contains("api-key")
                || lower.contains("session")
                || lower.contains("csrf")
                || lower.contains("xsrf")
                || lower.contains("idcard");
    }
}