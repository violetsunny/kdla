package top.kdla.framework.common.help;

import org.slf4j.MDC;

/**
 * @author YiHui
 * @date 2023/5/29
 */
public class MdcHelp {
    public static final String TRACE_ID_KEY = "traceId";

    public static void add(String key, String val) {
        MDC.put(key, val);
    }

    public static void addTraceId() {
        MDC.put(TRACE_ID_KEY, SelfSnowflakeGeneratorHelp.generate());
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void resetTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        MDC.clear();
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static void clear() {
        MDC.clear();
    }

    /**
     * 恢复指定 MDC 键的旧值：旧值为 null 时移除该键。
     * 用于切面 finally 中精确还原被覆盖的键，替代 MDC.clear() 整体清空，
     * 避免嵌套场景丢失外层上下文。
     *
     * @param key    MDC 键
     * @param oldVal 进入切面前保存的旧值，可为 null
     */
    public static void restore(String key, String oldVal) {
        if (oldVal == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, oldVal);
        }
    }
}
