package top.kdla.framework.catchlog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import top.kdla.framework.common.constants.CommonConstants;
import top.kdla.framework.common.utils.RequestUtil;
import top.kdla.framework.exception.BaseException;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.exception.SysException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;

/**
 *  Catching and Logging
 * @author kll
 * @since 2021/7/9 14:15
 */
@Aspect
@Slf4j
public class CatchLogAspect {

    /**
     * The syntax of pointcut : https://blog.csdn.net/zhengchao1991/article/details/53391244
     * 用于匹配所以持有指定注解CatchAndLog类型内的方法
     */
    @Pointcut("@within(top.kdla.framework.catchlog.CatchAndLog) && execution(public * *(..))")
    public void pointcut() {
    }

    /**
     * 方法调用之前调用，给打印日志添加一个traceId，方便查找问题
     *
     * @param joinPoint
     */
    @Before(value = "pointcut()")
    public void doBefore(JoinPoint joinPoint) {
        //添加traceId
        if (StringUtils.isEmpty(MDC.get(CommonConstants.LOG_TRACE_ID))) {
            JSONObject traceIdInfo = new JSONObject();
            traceIdInfo.put(CommonConstants.LOG_TRACE_ID, RequestUtil.getTraceId());
            MDC.put(CommonConstants.LOG_TRACE_ID, traceIdInfo.toJSONString());
        }

    }

    @Around(value = "pointcut()")
    public Object around(ProceedingJoinPoint joinPoint ) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();
        List<Object> args = filterArgs(joinPoint.getArgs());
        String clazzMethodInfo =  method.getDeclaringClass().getName().concat(".").concat(method.getName());
        log.info("====== {} Invoke start ======", clazzMethodInfo);
        StopWatch sw = new StopWatch(clazzMethodInfo);
        sw.start();
        Object response = null;
        try {
             response = joinPoint.proceed();
        } catch (Throwable e) {
            response = handleException(joinPoint, e);
        } finally {
            sw.stop();
            log.info("### {} Invoke Finished, requestParam: {}, response: {}, shortSummary:{}", clazzMethodInfo, JSON.toJSONString(args), JSON.toJSONString(response), sw.shortSummary());
            if (!LogTraceHolder.get()) {
                MDC.remove(CommonConstants.LOG_TRACE_ID);
                LogTraceHolder.remove();
            }
        }

        return response ;
    }

    /**
     * 处理异常
     * @param joinPoint
     * @param e
     * @return
     */
    private Object handleException(ProceedingJoinPoint joinPoint, Throwable e) {
        MethodSignature ms = (MethodSignature)joinPoint.getSignature();
        Method method = ms.getMethod();
        String clazzMethodInfo =  method.getDeclaringClass().getName().concat(".").concat(method.getName());
        log.warn(clazzMethodInfo + " invoke failed,exception is:", e);
        Class returnType = ms.getReturnType();

        if (e instanceof BizException) {
            log.warn(clazzMethodInfo+",BIZ EXCEPTION : {}" ,e.getMessage());
            return ResponseHandler.handle(returnType, (BaseException)e);
        }

        if (e instanceof SysException) {
            log.warn(clazzMethodInfo+",SYS EXCEPTION :", e);
            return ResponseHandler.handle(returnType, (BaseException)e);
        }
        log.error(clazzMethodInfo+",UNKNOWN EXCEPTION :", e);
        return ResponseHandler.handle(returnType, "UNKNOWN_ERROR", e.getMessage());
    }

    private List<Object> filterArgs(Object[] origArgs) {
        List<Object> filteredArgs = Lists.newArrayList();
        try {
            if (origArgs != null && origArgs.length > 0) {
                for (Object arg : origArgs) {
                    if (arg instanceof HttpServletRequest
                        || arg instanceof MultipartFile
                        || arg instanceof HttpServletResponse) {
                        continue;
                    }
                    filteredArgs.add(arg);
                }
            }
        } catch (Exception e) {
            log.warn("filterArgs execute Exception:", e);
        }

        return filteredArgs;
    }


}
