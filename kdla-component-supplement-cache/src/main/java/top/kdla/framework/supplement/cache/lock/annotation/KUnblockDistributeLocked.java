package top.kdla.framework.supplement.cache.lock.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁注解类
 *
 * @author kll
 * @date 2021-05-17
 */
@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KUnblockDistributeLocked {
    /**
     * 分布式锁key值
     *
     * @return
     */
    String key();

    /**
     * 锁超时时间 毫秒
     * @return
     */
    long timeout() default 5000;

    /**
     * 锁自动释放时间 毫秒
     * @return
     */
    long leaseTime() default 30000;

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
