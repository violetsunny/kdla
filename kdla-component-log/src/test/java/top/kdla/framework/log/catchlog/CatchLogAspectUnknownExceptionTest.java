package top.kdla.framework.log.catchlog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.kdla.framework.dto.Response;
import top.kdla.framework.exception.BaseException;
import top.kdla.framework.log.LogTraceHolder;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * 回归：未知异常曾把e.getMessage()透传调用方（SQL/路径外泄），BaseException分支被删落入未知错误。
 * 修复后：未知异常固定文案；BaseException子类按自身code处理。
 */
class CatchLogAspectUnknownExceptionTest {

    static class ResponseService {
        public Response doWork() {
            return Response.buildSuccess();
        }
    }

    static class CustomBaseException extends BaseException {
        CustomBaseException() {
            super("C-42", "custom base error");
        }
    }

    private CatchLogAspect aspect;
    private ProceedingJoinPoint pjp;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        aspect = new CatchLogAspect();
        pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = ResponseService.class.getMethod("doWork");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getReturnType()).thenReturn((Class) Response.class);
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @AfterEach
    void tearDown() {
        MDC.remove(LOG_TRACE_ID);
        LogTraceHolder.remove();
    }

    @Test
    void around_unknownException_fixedMessageNotLeaked() throws Throwable {
        when(pjp.proceed()).thenThrow(new RuntimeException("SELECT password FROM sys_user"));

        Object result = aspect.around(pjp);

        assertTrue(result instanceof Response);
        Response response = (Response) result;
        assertFalse(response.isSuccess());
        // 修复前：msg为异常原始message，SQL细节外泄
        assertEquals("系统繁忙，请稍后重试", response.getMsg(), "未知异常必须固定文案");
        assertFalse(response.getMsg().contains("SELECT"), "异常细节不得透传调用方");
    }

    @Test
    void around_customBaseException_handledWithOwnCode() throws Throwable {
        when(pjp.proceed()).thenThrow(new CustomBaseException());

        Object result = aspect.around(pjp);

        assertTrue(result instanceof Response);
        Response response = (Response) result;
        // 修复前：BaseException分支缺失，落入未知错误code=996
        assertEquals("C-42", response.getCode(), "BaseException子类必须按自身code处理");
        assertEquals("custom base error", response.getMsg());
    }
}