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
}
