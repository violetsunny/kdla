/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.log.requestlog;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.io.InputStreamSource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import top.kdla.framework.common.utils.JacksonUtil;
import top.kdla.framework.common.utils.KdlaStringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author kanglele
 * @version $Id: RequestLogAspect, v 0.1 2023/7/11 10:06 kanglele Exp $
 */
@Aspect
@Slf4j
public class RequestLogAspect {

    public RequestLogAspect() {
    }

    @Around("execution(!static top.kdla.framework.dto.Response *(..)) && (@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController))")
    public Object aroundApi(ProceedingJoinPoint point) throws Throwable {
        MethodSignature ms = (MethodSignature) point.getSignature();
        Method method = ms.getMethod();
        Object[] args = point.getArgs();
        Map<String, Object> paraMap = new HashMap(16);

        String paraName;
        for (int i = 0; i < args.length; ++i) {
            MethodParameter methodParam = new SynthesizingMethodParameter(method, i);
            methodParam.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
            PathVariable pathVariable = methodParam.getParameterAnnotation(PathVariable.class);
            if (pathVariable == null) {
                RequestBody requestBody = methodParam.getParameterAnnotation(RequestBody.class);
                String parameterName = methodParam.getParameterName();
                Object value = args[i];
                if (requestBody != null && value != null) {
                    paraMap.putAll(BeanUtil.beanToMap(value));
                } else {
                    if (value instanceof List) {
                        value = ((List) value).get(0);
                    }
                    if (value instanceof HttpServletRequest) {
                        paraMap.putAll(((HttpServletRequest) value).getParameterMap());
                    } else if (value instanceof WebRequest) {
                        paraMap.putAll(((WebRequest) value).getParameterMap());
                    } else if (value instanceof MultipartFile) {
                        MultipartFile multipartFile = (MultipartFile) value;
                        paraName = multipartFile.getName();
                        String fileName = multipartFile.getOriginalFilename();
                        paraMap.put(paraName, fileName);
                    } else if (!(value instanceof HttpServletResponse) && !(value instanceof InputStream) && !(value instanceof InputStreamSource)) {
                        if (value instanceof List) {
                            List<?> list = (List) value;
                            AtomicBoolean isSkip = new AtomicBoolean(false);
                            Iterator var14 = list.iterator();

                            while (var14.hasNext()) {
                                Object o = var14.next();
                                if ("StandardMultipartFile".equalsIgnoreCase(o.getClass().getSimpleName())) {
                                    isSkip.set(true);
                                    break;
                                }
                            }

                            if (isSkip.get()) {
                                paraMap.put(parameterName, "此参数不能序列化为json");
                            }
                        } else {
                            RequestParam requestParam = methodParam.getParameterAnnotation(RequestParam.class);
                            if (requestParam != null && KdlaStringUtil.isNotBlank(requestParam.value())) {
                                paraName = requestParam.value();
                            } else {
                                paraName = methodParam.getParameterName();
                            }

                            paraMap.put(paraName, value);
                        }
                    }
                }
            }
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes == null ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
        String requestURI = Objects.requireNonNull(request).getRequestURI();
        String requestMethod = request.getMethod();
        StringBuilder beforeReqLog = new StringBuilder(300);
        List<Object> beforeReqArgs = new ArrayList<>();
        beforeReqLog.append("\n\n================  Request Start  ================\n");
        beforeReqLog.append("===> {}: {}");
        beforeReqArgs.add(requestMethod);
        beforeReqArgs.add(requestURI);
        if (paraMap.isEmpty()) {
            beforeReqLog.append("\n");
        } else {
            beforeReqLog.append(" Parameters: {}\n");
            beforeReqArgs.add(JacksonUtil.toJson(paraMap));
        }

        Enumeration headers = request.getHeaderNames();

        while (headers.hasMoreElements()) {
            String headerName = (String) headers.nextElement();
            paraName = request.getHeader(headerName);
            beforeReqLog.append("===Headers===  {} : {}\n");
            beforeReqArgs.add(headerName);
            beforeReqArgs.add(paraName);
        }

        beforeReqLog.append("================  Request End   ================\n");
        long startNs = System.nanoTime();
        if (log.isInfoEnabled()) {
            log.info(beforeReqLog.toString(), beforeReqArgs.toArray());
        }
        StringBuilder afterReqLog = new StringBuilder(200);
        List<Object> afterReqArgs = new ArrayList<>();
        afterReqLog.append("\n\n================  Response Start  ================\n");
        boolean var24 = false;

        Object var17;
        try {
            var24 = true;
            Object result = point.proceed();
            afterReqLog.append("===Result===  {}\n");
            afterReqArgs.add(JacksonUtil.toJson(result));
            var17 = result;
            var24 = false;
        } finally {
            if (var24) {
                long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                afterReqLog.append("<=== {}: {} ({} ms)\n");
                afterReqArgs.add(requestMethod);
                afterReqArgs.add(requestURI);
                afterReqArgs.add(tookMs);
                afterReqLog.append("================  Response End   ================\n");
                if (log.isInfoEnabled()) {
                    log.info(afterReqLog.toString(), afterReqArgs.toArray());
                }
            }
        }

        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        afterReqLog.append("<=== {}: {} ({} ms)\n");
        afterReqArgs.add(requestMethod);
        afterReqArgs.add(requestURI);
        afterReqArgs.add(tookMs);
        afterReqLog.append("================  Response End   ================\n");
        if (log.isInfoEnabled()) {
            log.info(afterReqLog.toString(), afterReqArgs.toArray());
        }
        return var17;
    }
}
