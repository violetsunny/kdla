package top.kdla.framework.common.utils;

import java.util.UUID;

/**
 * @author kll
 * @Description Request工具类
 * @since 2021/7/13
 */
public class RequestUtil {
    /**
     * 获取一个请求TRACE_ID，做接口追踪使用
     */
    public static String getTraceId() {

        return getUuid();
    }

    /**
     * 获取一个uuid,做随机字符串
     */
    public static String getUuid() {

        return UUID.randomUUID().toString();
    }
}
