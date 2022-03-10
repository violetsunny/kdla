package top.kdla.framework.catchlog;

import java.util.Objects;

/**
 * @author vincent.li
 * @Description 日志
 * @since 2021/7/19
 */
public class LogTraceHolder {

    public static ThreadLocal<Boolean> holder = new ThreadLocal<>();

    public static boolean get() {
        if (Objects.isNull(holder.get())) {
            holder.set(Boolean.FALSE);
        }
        return holder.get();
    }

    public static void set(Boolean data) {
         holder.set(data);
    }

    public static void remove() {
        holder.remove();
    }




}
