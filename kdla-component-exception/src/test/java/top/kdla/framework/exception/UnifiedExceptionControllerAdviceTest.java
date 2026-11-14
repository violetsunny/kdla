package top.kdla.framework.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.ContentCachingRequestWrapper;
import top.kdla.framework.dto.Response;
import top.kdla.framework.dto.exception.ErrorCode;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 回归测试：
 * 1. 旧实现误将 java.nio.file.AccessDeniedException（文件 IO 异常）当作鉴权异常处理并返回 401；
 *    修复后由内部静态 advice 处理 spring-security 的 AccessDeniedException，HTTP 状态码修正为 403，
 *    且仅在宿主 classpath 存在 spring-security 时注册。
 * 2. handleHttpMessageNotReadableException 旧实现直接用 getCharacterEncoding()（可能为 null）构造字符串导致 NPE；
 *    修复后回退 UTF-8。
 * 3. handleThrowable 兜底旧实现把 exception.getMessage() 原样返回前端（可能泄露 SQL/内部路径等敏感信息）；
 *    修复后返回固定文案，原始详情只进 log.error。
 */
class UnifiedExceptionControllerAdviceTest {

    private final UnifiedExceptionControllerAdvice advice = new UnifiedExceptionControllerAdvice();

    @Test
    void handleThrowable_returnsFixedMessage_notRawExceptionMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        Response response = advice.handleThrowable(request,
                new RuntimeException("SQL: select * from t_user where pwd=123456"));

        assertEquals(ErrorCode.SYS_ERROR.getCode(), response.getCode());
        assertEquals("系统繁忙，请稍后重试", response.getMsg(), "兜底异常必须返回固定文案");
        assertFalse(response.getMsg().contains("SQL"), "底层异常详情禁止透出前端");
    }

    @Test
    void handleHttpMessageNotReadable_nullEncoding_fallsBackToUtf8() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", "/api/test");
        // MockHttpServletRequest 默认 characterEncoding 为 null，复现旧实现的 NPE 场景
        mockRequest.setContent("中文请求体".getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(mockRequest);
        // 触发内容缓存
        StreamUtils.copyToByteArray(wrapper.getInputStream());

        MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad json", inputMessage);

        Response response = advice.handleHttpMessageNotReadableException(ex, wrapper);

        assertNotNull(response, "编码为 null 时必须回退 UTF-8 正常返回，不能抛 NPE");
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), response.getCode());
    }

    @Test
    void securityAdvice_handlesSpringSecurityAccessDenied_with403() throws Exception {
        UnifiedExceptionControllerAdvice.SecurityAccessDeniedExceptionAdvice securityAdvice =
                new UnifiedExceptionControllerAdvice.SecurityAccessDeniedExceptionAdvice();

        Response response = securityAdvice.handleAccessDeniedException(new AccessDeniedException("denied"));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), response.getCode());

        Method handler = UnifiedExceptionControllerAdvice.SecurityAccessDeniedExceptionAdvice.class
                .getMethod("handleAccessDeniedException", AccessDeniedException.class);
        assertEquals(HttpStatus.FORBIDDEN, handler.getAnnotation(ResponseStatus.class).value(),
                "spring-security 权限异常的 HTTP 状态码必须是 403 FORBIDDEN");
    }

    @Test
    void outerAdvice_noLongerHandlesNioAccessDeniedException() {
        for (Method method : UnifiedExceptionControllerAdvice.class.getMethods()) {
            ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
            if (annotation == null) {
                continue;
            }
            for (Class<?> handled : annotation.value()) {
                assertNotEquals(java.nio.file.AccessDeniedException.class, handled,
                        "外层 advice 不能再把 java.nio.file.AccessDeniedException 当作鉴权异常处理");
            }
        }
    }
}
