/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.monitor.prometheus.annotation;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import top.kdla.framework.supplement.monitor.prometheus.PushGatewayManager;

import java.lang.reflect.Method;

/**
 * @author kanglele
 * @version $Id: PrometheusPointAspect, v 0.1 2023/5/31 10:08 kanglele Exp $
 */
@Slf4j
@Aspect
public class PrometheusPointAspect {

    @Pointcut("@annotation(top.kdla.framework.supplement.monitor.prometheus.annotation.PrometheusCounter) && execution(public * *(..))")
    public void pointcut() {
        log.info("--- PrometheusPointAspect start ---");
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //String className = joinPoint.getTarget().getClass().getSimpleName();
        Class<?> classTarget = joinPoint.getTarget().getClass();
        String methodName = joinPoint.getSignature().getName();
        Class<?>[] par = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();

        Method objMethod = classTarget.getMethod(methodName, par);

        PrometheusCounter prometheusCounter = objMethod.getAnnotation(PrometheusCounter.class);
        CollectorRegistry registry = PushGatewayManager.getRegistry();
        Counter counter = Counter.build(prometheusCounter.name(), prometheusCounter.help()).labelNames("type").register(registry);
        counter.labels(prometheusCounter.type()).inc();
        Object ob = joinPoint.proceed();
        log.info("{} {} 进行统计打点", prometheusCounter.name(), prometheusCounter.help());
        return ob;
    }

}
