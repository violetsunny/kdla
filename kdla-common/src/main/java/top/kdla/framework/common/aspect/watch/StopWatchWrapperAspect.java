/**
 * OYO.com Inc.
 * Copyright (c) 2017-2020 All Rights Reserved.
 */
package top.kdla.framework.common.aspect.watch;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import lombok.extern.slf4j.Slf4j;

/**
 * 计时统计
 * @author kanglele
 * @version $Id: ExceptionWrapperAspect, v 0.1 2020-01-03 17:10 Exp $
 */
@Slf4j
@Aspect
@Component
public class StopWatchWrapperAspect {

    @Pointcut("@annotation(top.kdla.framework.common.aspect.watch.StopWatchWrapper)")
    public void pointcut() {
        log.info("--- StopWatchWrapperAspect start ---");
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Class<?> classTarget=joinPoint.getTarget().getClass();
        String methodName = joinPoint.getSignature().getName();
        Class<?>[] par=((MethodSignature) joinPoint.getSignature()).getParameterTypes();

        Method objMethod=classTarget.getMethod(methodName, par);

        StopWatchWrapper stopWatchWrapper = objMethod.getAnnotation(StopWatchWrapper.class);
        String logTitle = stopWatchWrapper.logHead()+" "+className+" "+methodName+" "+stopWatchWrapper.msg();

        Stopwatch sw = Stopwatch.createStarted();
        Object ob = joinPoint.proceed();
        sw.stop();
        log.info(logTitle+"运行日志:{}", sw.toString());
        return ob;
    }
}
