package top.kdla.framework.common.aspect.watch;

import java.lang.annotation.*;

/**
 * 计时包装注解
 * @author kanglele
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StopWatchWrapper {

    /**
     * 日志头
     * @return
     */
    String logHead() default "";

    /**
     * 特有信息
     * @return
     */
    String msg() default "";
}
