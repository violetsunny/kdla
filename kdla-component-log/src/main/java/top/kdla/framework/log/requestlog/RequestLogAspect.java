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
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import top.kdla.framework.common.utils.JacksonUtil;
import top.kdla.framework.common.help.KdlaStringHelp;
import top.kdla.framework.log.sanitize.LogSanitizeUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

    /** 软引用缓存，避免每个请求每参数重读字节码；不可解析用 NO_NAMES 哨兵占位（Map 不允许 null 值） */
    private static final Map<Method, String[]> LVT_CACHE = new ConcurrentReferenceHashMap<>();
    private static final String[] NO_NAMES = new String[0];

    public RequestLogAspect() {
    }

    @Around("execution(!static top.kdla.framework.dto.Response *(..)) && (@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController))")
    public Object aroundApi(ProceedingJoinPoint point) throws Throwable {
        MethodSignature ms = (MethodSignature) point.getSignature();
        Method method = ms.getMethod();
        Object[] args = point.getArgs();
        Map<String, Object> paraMap = new HashMap(16);
        String requestURI = null;
        String requestMethod = null;
        long startNs = 0;
        StringBuilder afterReqLog = new StringBuilder(200);
        List<Object> afterReqArgs = new ArrayList<>();
        boolean var24 = false;

        DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
        // Spring6.1删除LVT发现器，宿主未开-parameters时回退读字节码LocalVariableTable，保住日志参数名（与MdcAspect同方案）
        nameDiscoverer.addDiscoverer(new ParameterNameDiscoverer() {
            @Override
            public String[] getParameterNames(Method m) {
                return lvtParameterNames(m);
            }

            @Override
            public String[] getParameterNames(Constructor<?> ctor) {
                return null;
            }
        });
        try {
            String paraName;
            for (int i = 0; i < args.length; ++i) {
                MethodParameter methodParam = new SynthesizingMethodParameter(method, i);
                methodParam.initParameterNameDiscovery(nameDiscoverer);
                PathVariable pathVariable = methodParam.getParameterAnnotation(PathVariable.class);
                if (pathVariable == null) {
                    RequestBody requestBody = methodParam.getParameterAnnotation(RequestBody.class);
                    String parameterName = methodParam.getParameterName();
                    Object value = args[i];
                    if (requestBody != null && value != null) {
                        paraMap.putAll(BeanUtil.beanToMap(value));
                    } else {
                        if (value instanceof List) {
                            List<?> list = (List<?>) value;
                            if (!list.isEmpty()) {
                                value = list.get(0);
                            }
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
                                if (requestParam != null && KdlaStringHelp.isNotBlank(requestParam.value())) {
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
            requestURI = Objects.requireNonNull(request).getRequestURI();
            requestMethod = request.getMethod();
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
                beforeReqArgs.add(LogSanitizeUtil.sanitize(JacksonUtil.toJson(paraMap)));
            }

            Enumeration headers = request.getHeaderNames();

            while (headers.hasMoreElements()) {
                String headerName = (String) headers.nextElement();
                // 敏感Header（authorization/cookie/password/token/mobile）值脱敏为***
                paraName = LogSanitizeUtil.maskHeader(headerName, request.getHeader(headerName));
                beforeReqLog.append("===Headers===  {} : {}\n");
                beforeReqArgs.add(headerName);
                beforeReqArgs.add(paraName);
            }

            beforeReqLog.append("================  Request End   ================\n");
            startNs = System.nanoTime();
            if (log.isInfoEnabled()) {
                log.info(beforeReqLog.toString(), beforeReqArgs.toArray());
            }
            afterReqLog.append("\n\n================  Response Start  ================\n");
        } catch (Exception e) {
            log.warn("request log aspect parse error, skip request log", e);
            return point.proceed();
        }

        Object var17;
        try {
            var24 = true;
            Object result = point.proceed();
            afterReqLog.append("===Result===  {}\n");
            // 完整响应超2KB截断，防止大报文刷爆日志
            afterReqArgs.add(LogSanitizeUtil.sanitize(JacksonUtil.toJson(result)));
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

    /**
     * 读字节码LocalVariableTable取参数名（spring-core内嵌ASM），与common模块MdcAspect同方案
     */
    private static String[] lvtParameterNames(Method method) {
        String[] names = LVT_CACHE.computeIfAbsent(method, m -> {
            String[] resolved = readLvtParameterNames(m);
            return resolved == null ? NO_NAMES : resolved;
        });
        return names == NO_NAMES ? null : names;
    }

    private static String[] readLvtParameterNames(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        String resource = clazz.getName().replace('.', '/') + ".class";
        ClassLoader loader = clazz.getClassLoader();
        try (InputStream in = loader == null ? ClassLoader.getSystemResourceAsStream(resource) : loader.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            String[] names = new String[method.getParameterCount()];
            int[] slots = new int[names.length];
            int slot = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
            for (int i = 0; i < names.length; i++) {
                slots[i] = slot;
                slot += Type.getType(method.getParameterTypes()[i]).getSize();
            }
            String descriptor = Type.getMethodDescriptor(method);
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if (!name.equals(method.getName()) || !desc.equals(descriptor)) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitLocalVariable(String varName, String desc, String signature, Label start, Label end, int index) {
                            for (int i = 0; i < slots.length; i++) {
                                if (slots[i] == index && names[i] == null) {
                                    names[i] = varName;
                                }
                            }
                        }
                    };
                }
            }, 0);
            for (String name : names) {
                if (name == null) {
                    return null;
                }
            }
            return names;
        } catch (Exception e) {
            return null;
        }
    }
}
