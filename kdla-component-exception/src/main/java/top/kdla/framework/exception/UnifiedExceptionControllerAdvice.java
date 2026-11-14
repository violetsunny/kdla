package top.kdla.framework.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.dao.DuplicateKeyException;
import top.kdla.framework.dto.Response;
import top.kdla.framework.dto.exception.ErrorCode;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用异常处理
 * 
 * <p>默认启用,可通过配置关闭:</p>
 * <pre>{@code
 * kdla:
 *   exception:
 *     handler:
 *       enabled: false
 * }</pre>
 *
 * @author kll
 * @since 2021-01-07 16:22
 **/
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "kdla.exception.handler.enabled",
                       havingValue = "true", matchIfMissing = true)
@Import(UnifiedExceptionControllerAdvice.SecurityAccessDeniedExceptionAdvice.class)
public class UnifiedExceptionControllerAdvice {

    /**
     * spring-security 鉴权/认证异常专属处理。
     * <p>
     * 历史 bug：外层曾误将 {@code java.nio.file.AccessDeniedException}（文件 IO 异常）
     * 当作鉴权异常处理并返回 401。现改为处理 spring-security 的
     * {@link AccessDeniedException}，HTTP 状态码修正为 403 FORBIDDEN，
     * 且仅在宿主 classpath 存在 spring-security 时才注册（@ConditionalOnClass），
     * 未引入 security 的宿主不受影响。
     * <p>
     * 同类 bug：外层曾误绑 {@code javax.security.sasl.AuthenticationException}（SASL 异常，
     * 与 spring-security 的 AuthenticationException 无继承关系），导致认证失败落入
     * handleThrowable 兜底返回"系统繁忙"而非 401。现移入本内层 Advice 处理正确类型。
     */
    @RestControllerAdvice
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnClass(AccessDeniedException.class)
    public static class SecurityAccessDeniedExceptionAdvice {

        @ResponseStatus(HttpStatus.FORBIDDEN)
        @ExceptionHandler(AccessDeniedException.class)
        public Response handleAccessDeniedException(AccessDeniedException e) {
            if (log.isWarnEnabled()) {
                log.warn("handleAccessDeniedException:{}", ExceptionUtils.getStackTrace(e));
            }
            return Response.buildFailure(ErrorCode.UNAUTHORIZED);
        }

        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
        public Response handleAuthenticationException(org.springframework.security.core.AuthenticationException e) {
            if (log.isWarnEnabled()) {
                log.warn("handleAuthenticationException:{}", ExceptionUtils.getStackTrace(e));
            }
            return Response.buildFailure(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 统一参数验证异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Response validExceptionHandler(MethodArgumentNotValidException e, HttpServletRequest request) {
        if (log.isWarnEnabled()) {
            log.warn("req: {},MethodArgumentNotValidException: {}", request.getRequestURI(), ExceptionUtils.getStackTrace(e));
        }
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        List<String> errors = allErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList());
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), errors.toString());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    @ResponseBody
    public Response handleBizException(HttpServletRequest req, Throwable exception) {
        if (log.isWarnEnabled()) {
            log.warn("req: {},BizException:{}", req.getRequestURI(), ExceptionUtils.getStackTrace(exception));
        }
        BizException bizException = (BizException) exception;
        String errorCode = bizException.getCode();
        errorCode = ErrorCode.BIZ_ERROR.getCode().equalsIgnoreCase(errorCode) ? ErrorCode.BAD_REQUEST.getCode() : errorCode;
        String errorMessage = bizException.getMessage();
        return Response.buildFailure(errorCode, errorMessage);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(SysException.class)
    @ResponseBody
    public Response handleSysException(HttpServletRequest req, Throwable exception) {
        if (log.isWarnEnabled()) {
            log.warn("req: {},SysException:{}", req.getRequestURI(), ExceptionUtils.getStackTrace(exception));
        }
        SysException sysException = (SysException) exception;
        String errorCode = sysException.getCode();
        errorCode = ErrorCode.SYS_ERROR.getCode().equalsIgnoreCase(errorCode) ? ErrorCode.FAIL.getCode() : errorCode;
        String errorMessage = sysException.getMessage();
        return Response.buildFailure(errorCode, errorMessage);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Response handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("MaxUploadSizeExceededException:{}", ExceptionUtils.getStackTrace(e));
        return Response.buildFailure(ErrorCode.BEYOND_MAX_SIZE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public Response constraintViolationHandler(ConstraintViolationException ex, HttpServletRequest request) {
        if (log.isWarnEnabled()) {
            log.warn("req: {},ConstraintViolationException:{}", request.getRequestURI(), ExceptionUtils.getStackTrace(ex));
        }
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), errors.toString());

    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(InvalidParameterException.class)
    public Response handleInvalidParameterException(InvalidParameterException ex, HttpServletRequest request) {
        if (log.isWarnEnabled()) {
            log.warn("req: {},InvalidParameterException:{}", request.getRequestURI(), ExceptionUtils.getStackTrace(ex));
        }
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), ex.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(IllegalArgumentException.class)
    public Response handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        if (log.isWarnEnabled()) {
            log.warn("req: {},IllegalArgumentException:{}", request.getRequestURI(), ExceptionUtils.getStackTrace(ex));
        }
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), ex.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(DuplicateKeyException.class)
    public Response handleDuplicateKeyException(DuplicateKeyException ex) {
        if (log.isWarnEnabled()) {
            log.warn("DuplicateKeyException:{}", ExceptionUtils.getStackTrace(ex));
        }
        return Response.buildFailure(ErrorCode.DUPLICATE_KEY);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Response handlerMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        if (log.isWarnEnabled()) {
            log.warn("MissingServletRequestParameterException:{}", ExceptionUtils.getStackTrace(ex));
        }
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), ex.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) throws Exception {
        String requestBody = "";
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper requestWrapper = (ContentCachingRequestWrapper) request;
            // getCharacterEncoding() 可能返回 null，回退 UTF-8，避免 new String(bytes, null) 抛 NPE
            String characterEncoding = requestWrapper.getCharacterEncoding();
            if (characterEncoding == null) {
                characterEncoding = StandardCharsets.UTF_8.name();
            }
            requestBody = new String(requestWrapper.getContentAsByteArray(), characterEncoding);
        }
        log.error("req: {},body: {},HttpMessageNotReadableException:{}", request.getRequestURI(), requestBody, ExceptionUtils.getStackTrace(ex));
        return Response.buildFailure(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public Response handleThrowable(HttpServletRequest req, Throwable exception) {
        log.error("handleThrowable,url:{},Throwable:{}", req.getRequestURI(), ExceptionUtils.getStackTrace(exception));
        String errorMessage = exception.getMessage();
        if (exception instanceof ConstraintViolationException) {
            return Response.buildFailure(ErrorCode.FAIL.getCode(), errorMessage);
        }
        if (exception instanceof BindException) {
            return Response.buildFailure(ErrorCode.FAIL.getCode(), errorMessage);
        }
        // 兜底异常不把 exception.getMessage() 原样透出前端（可能含 SQL/内部路径等敏感信息），原始详情只进上方 log.error
        return Response.buildFailure(ErrorCode.SYS_ERROR.getCode(), "系统繁忙，请稍后重试");
    }

}
