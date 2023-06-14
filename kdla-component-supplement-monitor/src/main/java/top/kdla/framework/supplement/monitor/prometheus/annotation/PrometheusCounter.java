package top.kdla.framework.supplement.monitor.prometheus.annotation;

import java.lang.annotation.*;

/**
 * Prometheus打点
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PrometheusCounter {
    /**
     * 上报名称
     * @return
     */
    String name() default "";
    /**
     * 上报信息
     * @return
     */
    String help() default "";
    /**
     * 上报类型
     * @return
     */
    String type() default "";
    /**
     * 特有信息
     * @return
     */
    String msg() default "";

}
