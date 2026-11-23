package top.kdla.framework.log.catchlog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.kdla.framework.dto.Response;
import top.kdla.framework.exception.BizException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：around 曾把 catch(Throwable) 的全部异常都转成 Response 返回，
 * 导致返回类型不是 Response 的方法（如 String）拿到类型不匹配的对象（ClassCastException 风险），
 * 且 Error（OOM/StackOverflow）也被吞掉。
 * 修复后：Error 无条件 rethrow；仅返回类型可赋值为 Response 时才转换异常，否则原样上抛。
 */
class CatchLogAspectAroundRethrowTest {

    /** 被切方法返回类型不是Response的代表 */
    static class StringService {
        public String doWork() {
            return "ok";
        }
    }

    /** 被切方法返回类型是Response的代表 */
    static class ResponseService {
        public Response doWork() {
            return Response.buildSuccess();
        }
    }

    private CatchLogAspect aspect;
    private ProceedingJoinPoint pjp;
    private MethodSignature signature;

    @BeforeEach
    void setUp() {
        aspect = new CatchLogAspect();
        pjp = mock(ProceedingJoinPoint.class);
        signature = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[0]);
    }

    private void stubMethod(Class<?> clazz, Class<?> returnType) throws Exception {
        Method method = clazz.getMethod("doWork");
        when(signature.getMethod()).thenReturn(method);
        when(signature.getReturnType()).thenReturn(returnType);
    }

    @Test
    @SuppressWarnings("unchecked")
    void around_nonResponseReturnType_exceptionPropagates() throws Throwable {
        stubMethod(StringService.class, String.class);
        RuntimeException boom = new RuntimeException("boom");
        when(pjp.proceed()).thenThrow(boom);

        // 修复前：异常被吞，返回一个Response对象（类型不匹配）
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> aspect.around(pjp),
                "返回String的方法抛RuntimeException时异常必须向上传播，不得转成Response");
        assertTrue(thrown == boom, "必须原样上抛同一个异常实例");
    }

    @Test
    @SuppressWarnings("unchecked")
    void around_responseReturnType_bizExceptionConvertedToFailureResponse() throws Throwable {
        stubMethod(ResponseService.class, Response.class);
        when(pjp.proceed()).thenThrow(new BizException("biz error"));

        // 返回Response的方法抛BizException时，保持原有转换行为
        Object result = aspect.around(pjp);

        assertTrue(result instanceof Response, "返回类型为Response时必须转换为失败Response");
        assertFalse(((Response) result).isSuccess(), "转换结果必须是失败状态");
    }

    @Test
    @SuppressWarnings("unchecked")
    void around_errorAlwaysRethrows_evenForResponseReturnType() throws Throwable {
        stubMethod(ResponseService.class, Response.class);
        StackOverflowError oom = new StackOverflowError("fatal");
        when(pjp.proceed()).thenThrow(oom);

        // 修复前：Error也被handleException吞掉转成Response
        Error thrown = assertThrows(StackOverflowError.class, () -> aspect.around(pjp),
                "Error必须无条件rethrow，禁止转Response吞掉");
        assertTrue(thrown == oom, "必须原样上抛同一个Error实例");
    }
}
