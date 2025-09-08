package top.kdla.framework.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
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
import org.yaml.snakeyaml.constructor.DuplicateKeyException;
import top.kdla.framework.dto.Response;
import top.kdla.framework.dto.exception.ErrorCode;

import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用异常处理
 *
 * @author kll
 * @since 2021-01-07 16:22
 **/
@Slf4j
@RestControllerAdvice
public class UnifiedExceptionControllerAdvice {

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public Response handleAuthenticationException(AuthenticationException e) {
        if (log.isWarnEnabled()) {
            log.warn("handleAuthenticationException:{}", ExceptionUtils.getStackTrace(e));
        }
        return Response.buildFailure(ErrorCode.UNAUTHORIZED);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AccessDeniedException.class)
    public Response handleAccessDeniedException(AccessDeniedException e) {
        if (log.isWarnEnabled()) {
            log.warn("handleAccessDeniedException:{}", ExceptionUtils.getStackTrace(e));
        }
        return Response.buildFailure(ErrorCode.UNAUTHORIZED);
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
        return Response.buildFailure(ErrorCode.DUPLICATE_KEY.getCode(), ex.getMessage());
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
            requestBody = new String(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
        }
        log.error("req: {},body: {},HttpMessageNotReadableException:{}", request.getRequestURI(), requestBody, ExceptionUtils.getStackTrace(ex));
        return Response.buildFailure(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    /**
     * 其他异常
     */
//    @ExceptionHandler(Throwable.class)
//    @ResponseBody
//    public Response handleThrowable(HttpServletRequest req, Throwable exception) {
//        log.error("handleThrowable,url:{},Throwable:{}", req.getRequestURI(), ExceptionUtils.getStackTrace(exception));
//        String errorMessage = exception.getMessage();
//        if (exception instanceof ConstraintViolationException) {
//            return Response.buildFailure(ErrorCode.FAIL.getCode(), errorMessage);
//        }
//        if (exception instanceof BindException) {
//            return Response.buildFailure(ErrorCode.FAIL.getCode(), errorMessage);
//        }
//        return Response.buildFailure(ErrorCode.SYS_ERROR.getCode(), errorMessage);
//    }

    @ExceptionHandler({Throwable.class})
    @ResponseBody
    public Response handleThrowable(WebRequest request, Throwable exception) {
        String uri = request.getDescription(false); // 获取请求URI
        log.error("handleThrowable,url:{},Throwable:{}", uri, ExceptionUtils.getStackTrace(exception));
        // 隐藏敏感异常信息，返回通用错误信息给前端
        if (exception instanceof ConstraintViolationException || exception instanceof BindException) {
            String errorMessage = exception.getMessage();
            if (StringUtils.hasText(errorMessage)) {
                log.warn("参数校验异常, url: {}, message: {}", uri, errorMessage);
                return Response.buildFailure(ErrorCode.FAIL.getCode(), "参数校验失败");
            }
            return Response.buildFailure(ErrorCode.FAIL.getCode(), "参数校验失败");
        } else {
            // 记录详细错误信息到日志，但不返回给前端
            log.error("系统异常, url: {}", uri, exception);
            return Response.buildFailure(ErrorCode.SYS_ERROR.getCode(), "系统内部错误");
        }
    }
}
