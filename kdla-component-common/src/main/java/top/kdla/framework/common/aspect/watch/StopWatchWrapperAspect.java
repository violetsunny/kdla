/**
 * OYO.com Inc.
 * Copyright (c) 2017-2020 All Rights Reserved.
 */
package top.kdla.framework.common.aspect.watch;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 计时统计
 *
 * @author kanglele
 * @version $Id: ExceptionWrapperAspect, v 0.1 2020-01-03 17:10 Exp $
 */
@Slf4j
@Aspect
public class StopWatchWrapperAspect {

    @Value("${kdla.stop.watch.error.timeout:2000}")
    private Integer errorTimeOut;

    @Value("${kdla.stop.watch.warn.timeout:200}")
    private Integer warnTimeOut;

    @Pointcut("@annotation(top.kdla.framework.common.aspect.watch.StopWatchWrapper) && execution(public * *(..))")
    public void pointcut() {
        if (log.isDebugEnabled()) {
            log.debug("--- StopWatchWrapperAspect start ---");
        }
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        // 直接用 signature.getMethod() 定位注解（AopUtils 取目标类最具体方法，兼容接口代理场景），
        // 替代 getTarget().getClass().getMethod(...) 反射重取（可能抛 NoSuchMethodException 且注解可能为 null）
        Method objMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method specificMethod = AopUtils.getMostSpecificMethod(objMethod, joinPoint.getTarget().getClass());
        StopWatchWrapper stopWatchWrapper = AnnotationUtils.findAnnotation(specificMethod, StopWatchWrapper.class);
        if (stopWatchWrapper == null) {
            // 找不到注解时跳过计时，直接执行业务方法
            return joinPoint.proceed();
        }
        String logTitle = stopWatchWrapper.logHead() + " " + className + " " + methodName + " " + stopWatchWrapper.msg();

        Stopwatch sw = Stopwatch.createStarted();
        Object ob;
        try {
            ob = joinPoint.proceed();
        } finally {
            sw.stop();
            //不同的级别打印日志不同
            if (sw.elapsed(TimeUnit.MILLISECONDS) > errorTimeOut) {
                if (log.isWarnEnabled()) {
                    log.warn(logTitle + " " + "接口超过" + errorTimeOut + "ms 运行:{}", sw.toString());
                }
            } else if (sw.elapsed(TimeUnit.MILLISECONDS) > warnTimeOut) {
                if (log.isWarnEnabled()) {
                    log.warn(logTitle + " " + "接口超过" + warnTimeOut + "ms 运行:{}", sw.toString());
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info(logTitle + " " + "运行:{}", sw.toString());
                }
            }
        }
        return ob;
    }
}
