package top.kdla.framework.log.catchlog;

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
import top.kdla.framework.common.help.MdcHelp;
import top.kdla.framework.common.help.SelfSnowflakeGeneratorHelp;
import top.kdla.framework.dto.Response;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.*;
import top.kdla.framework.log.LogTraceHolder;
import top.kdla.framework.log.sanitize.LogSanitizeUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Catching and Logging
 *
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
    @Pointcut("@within(top.kdla.framework.log.catchlog.CatchAndLog) && execution(public * *(..))")
    public void pointcut() {
        if (log.isDebugEnabled()) {
            log.debug("======CatchLogAspect start ======");
        }
    }

    /**
     * 方法调用之前调用，给打印日志添加一个traceId，方便查找问题
     *
     * @param joinPoint
     */
    @Before(value = "pointcut()")
    public void doBefore(JoinPoint joinPoint) {
        //添加traceId
        if (!StringUtils.hasText(MDC.get(CommonConstants.LOG_TRACE_ID))) {
            // 第一从属性中获取
            List<Object> args = filterArgs(joinPoint.getArgs());
            if (args.isEmpty()) {
                // 方法无参数（或全部参数被过滤掉），直接生成traceId
                MDC.put(CommonConstants.LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
                return;
            }
            Object trace = args.get(0);//默认取第一个
            JSONObject traceObj = null;
            try {
                if (trace instanceof String) {
                    traceObj = JSONObject.parseObject(String.valueOf(trace));
                } else if (trace instanceof JSONObject) {
                    traceObj = (JSONObject) trace;
                } else if (trace != null) {
                    traceObj = JSONObject.parseObject(JSONObject.toJSONBytes(trace));
                }
            } catch (Exception e) {
                // 首参不是合法JSON时降级为直接生成traceId，禁止让解析异常炸掉业务方法
                if (log.isWarnEnabled()) {
                    log.warn("doBefore parse first arg for traceId failed:", e);
                }
                MDC.put(CommonConstants.LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
                return;
            }
            if (traceObj == null) {
                MDC.put(CommonConstants.LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
                return;
            }
            String traceId = traceObj.getString(CommonConstants.LOG_TRACE_ID);
            if (StringUtils.hasText(traceId)) {
                MDC.put(CommonConstants.LOG_TRACE_ID, traceId);
            } else {
                // 自定义一个
                MDC.put(CommonConstants.LOG_TRACE_ID, SelfSnowflakeGeneratorHelp.generate());
            }
        }

    }

    @Around(value = "pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();
        List<Object> args = filterArgs(joinPoint.getArgs());
        String clazzMethodInfo = method.getDeclaringClass().getName().concat(".").concat(method.getName());
        // 覆盖前保存旧值并记录"是否本层放入"：around入口MDC为空说明本层的doBefore会放入traceId，
        // 嵌套@CatchAndLog时内层入口MDC已有值（外层放入），内层finally不得清理（与MdcAspect保存-恢复模式一致）
        String oldTraceId = MDC.get(CommonConstants.LOG_TRACE_ID);
        boolean tracePutByThisLayer = !StringUtils.hasText(oldTraceId);
        StopWatch sw = new StopWatch(clazzMethodInfo);
        sw.start();
        Object response = null;
        try {
            response = joinPoint.proceed();
        } catch (Throwable e) {
            // Error（OOM/StackOverflow等）无条件放行，禁止吞掉转Response
            if (e instanceof Error) {
                throw (Error) e;
            }
            // 仅当被切方法返回类型是Response（含子类）时才把异常转换为失败Response，否则原样上抛
            if (!Response.class.isAssignableFrom(ms.getReturnType())) {
                throw e;
            }
            response = handleException(joinPoint, e);
        } finally {
            sw.stop();
            if (log.isInfoEnabled()) {
                log.info("### {} Invoke Finished, requestParam: {}, response: {}, shortSummary:{}", clazzMethodInfo, LogSanitizeUtil.truncate(JSON.toJSONString(args)), LogSanitizeUtil.truncate(JSON.toJSONString(response)), sw.shortSummary());
            }
            // 仅本层放入traceId时才负责清理，禁止内层清掉外层的traceId
            if (!LogTraceHolder.get() && tracePutByThisLayer) {
                MdcHelp.restore(CommonConstants.LOG_TRACE_ID, oldTraceId);
                LogTraceHolder.remove();
            }
        }

        return response;
    }

    /**
     * 处理异常
     *
     * @param joinPoint
     * @param e
     * @return
     */
    private Object handleException(ProceedingJoinPoint joinPoint, Throwable e) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();
        String clazzMethodInfo = method.getDeclaringClass().getName().concat(".").concat(method.getName());
        if (log.isWarnEnabled()) {
            log.warn(clazzMethodInfo + " invoke failed,exception is:", e);
        }
        Class returnType = ms.getReturnType();
        if (e instanceof BizException) {
            if (log.isWarnEnabled()) {
                log.warn("{},BIZ EXCEPTION : {}", clazzMethodInfo, e.getMessage());
            }
            return ResponseHandler.handle(returnType, (BaseException) e);
        }
        if (e instanceof SysException) {
            if (log.isWarnEnabled()) {
                log.warn("{},SYS EXCEPTION :", clazzMethodInfo, e);
            }
            return ResponseHandler.handle(returnType, (BaseException) e);
        }
        if (e instanceof LockFailException) {
            if (log.isWarnEnabled()) {
                log.warn("{},LockFail EXCEPTION :", clazzMethodInfo, e);
            }
            return ResponseHandler.handle(returnType, (BaseException) e);
        }
        // Expand coverage for any framework BaseException.
        if (e instanceof BaseException) {
            if (log.isWarnEnabled()) {
                log.warn("{},BASE EXCEPTION : {}", clazzMethodInfo, e.getMessage());
            }
            return ResponseHandler.handle(returnType, (BaseException) e);
        }
        log.error(clazzMethodInfo + ",UNKNOWN EXCEPTION :", e);
        // 未知异常message可能含SQL/内部路径等敏感信息，不外传调用方；详情只进上方log.error
        return ResponseHandler.handle(returnType, ErrorCode.UNKNOWN_ERROR.getCode(), "系统繁忙，请稍后重试");
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
            if (log.isWarnEnabled()) {
                log.warn("filterArgs execute Exception:", e);
            }
        }

        return filteredArgs;
    }


}
