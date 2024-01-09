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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import top.kdla.framework.supplement.monitor.prometheus.PushGatewayManager;

import java.lang.reflect.Method;

/**
 * @author kanglele
 * @version $Id: PrometheusPointAspect, v 0.1 2023/5/31 10:08 kanglele Exp $
 */
@Slf4j
@Aspect
@Order(9)
public class PrometheusPointAspect {

    @Value("${prometheus.service.switch:false}")
    private boolean service;

    private final PushGatewayManager pushGatewayManager;

    public PrometheusPointAspect(PushGatewayManager pushGatewayManager) {
        this.pushGatewayManager = pushGatewayManager;
    }

    @Pointcut("@annotation(top.kdla.framework.supplement.monitor.prometheus.annotation.PrometheusCounter) && execution(public * *(..))")
    public void pointcut() {
        if (log.isDebugEnabled()) {
            log.debug("--- PrometheusPointAspect start ---");
        }
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //先执行
        Object obj = joinPoint.proceed();

        //后置统计
        //String className = joinPoint.getTarget().getClass().getSimpleName();
        Class<?> classTarget = joinPoint.getTarget().getClass();
        String methodName = joinPoint.getSignature().getName();
        Class<?>[] par = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();

        Method objMethod = classTarget.getMethod(methodName, par);

        PrometheusCounter prometheusCounter = objMethod.getAnnotation(PrometheusCounter.class);
        CollectorRegistry registry = pushGatewayManager.getRegistry();
        //通过collector类下的register方法可以把测点注册到上端口中
        //Collector是测点集合，也可以同时有Counter等单独测点
        Counter counter = Counter.build(prometheusCounter.name(), prometheusCounter.help()).labelNames("type").register(registry);
        counter.labels(prometheusCounter.type()).inc();
        if (!service) {
            pushGatewayManager.pushAdd(counter);
        }

        if (log.isInfoEnabled()) {
            log.info("PrometheusCounter {} {} 进行统计打点", prometheusCounter.name(), prometheusCounter.help());
        }
        return obj;
    }

}
