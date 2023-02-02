package top.kdla.framework.supplement.lock.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁注解类
 *
 * @author kll
 * @auther wangjyc@enn.cn
 * @date 2021-05-17
 */
@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UnblockDistributeLocked {
    /**
     * 分布式锁key值
     *
     * @return
     */
    String key();

    /**
     * key值是否为spel表达式
     *
     * @return
     */
    boolean isSpelKey() default false;

    /**
     * 未获取到锁是否抛异常
     *
     * @return
     */
    boolean throwEx() default true;

}
