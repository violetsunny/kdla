package top.kdla.framework.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;
import top.kdla.framework.dto.ErrorCode;
import top.kdla.framework.dto.Response;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.exception.SysException;

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
 * @since  2021-01-07 16:22
 **/
@Slf4j
@RestControllerAdvice
public class UnifiedExceptionControllerAdvice {

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public Response handleAuthenticationException(AuthenticationException ex) {
        log.warn("handleAuthenticationException:", ex);
        return Response.buildFailure(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMsg());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AccessDeniedException.class)
    public Response handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("handleAccessDeniedException:", ex);
        return Response.buildFailure(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMsg());
    }
    /**
     * 统一参数验证异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Response validExceptionHandler(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.info("The request [{}] MethodArgumentNotValidException: {}", request.getRequestURI(), ExceptionUtils.getStackTrace(e));
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
        log.info("handleBizException, req: {},BizException:{}", req.getRequestURI(), ExceptionUtils.getStackTrace(exception));
        BizException bizException = (BizException) exception;
        String errorCode = bizException.getCode();
        errorCode = ErrorCode.BIZ_ERROR.getCode().equalsIgnoreCase(errorCode) ? ErrorCode.BAD_REQUEST.getCode(): errorCode;
        String errorMessage = bizException.getMessage();
        return Response.buildFailure(errorCode, errorMessage);
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(SysException.class)
    @ResponseBody
    public Response handleSysException(HttpServletRequest req, Throwable exception) {
        log.info("handleSysException, req: {},SysException:{}", req.getRequestURI(), ExceptionUtils.getStackTrace(exception));
        SysException sysException = (SysException) exception;
        String errorCode = sysException.getCode();
        errorCode = ErrorCode.SYS_ERROR.getCode().equalsIgnoreCase(errorCode) ? ErrorCode.FAIL.getCode(): errorCode;
        String errorMessage = sysException.getMessage();
        return Response.buildFailure(errorCode, errorMessage);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Response handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("handleMaxUploadSizeExceededException,exception is:", e);
        return Response.buildFailure(ErrorCode.BEYOND_MAX_SIZE.getCode(), ErrorCode.BEYOND_MAX_SIZE.getMsg());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public Response constraintViolationHandler(ConstraintViolationException ex, HttpServletRequest request) {
        log.info("handleConstraintViolationException, req: {},ConstraintViolationException:{}", request.getRequestURI(), ExceptionUtils.getStackTrace(ex));
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
        log.info("handleInvalidParameterException, req: {},ConstraintViolationException:{}", request.getRequestURI(), ExceptionUtils.getStackTrace(ex));
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), ex.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(IllegalArgumentException.class)
    public Response handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.info("handleIllegalArgumentException, req: {},ConstraintViolationException:{}", request.getRequestURI(), ExceptionUtils.getStackTrace(ex));
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), ex.getMessage());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(DuplicateKeyException.class)
    public Response handleDuplicateKeyException(DuplicateKeyException ex) {
        log.error("handleDuplicateKeyException", ex);
        return Response.buildFailure(ErrorCode.UNKNOWN_ERROR.getCode(), ErrorCode.UNKNOWN_ERROR.getMsg());
    }


    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Response handlerMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("handlerMissingServletRequestParameterException", ex);
        return Response.buildFailure(ErrorCode.PARAMETER_ERROR.getCode(), ex.getMessage());
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public Response handleThrowable(HttpServletRequest req, Throwable exception) {
        log.error("handleThrowable, req: {},Throwable:{}", req.getRequestURI(), ExceptionUtils.getStackTrace(exception));
        String errorMessage = exception.getMessage();

        if (exception instanceof ConstraintViolationException) {
            return Response.buildFailure(ErrorCode.FAIL.getCode(), errorMessage);
        }

        if (exception instanceof BindException) {
            return Response.buildFailure(ErrorCode.FAIL.getCode(), errorMessage);
        }

        return Response.buildFailure(ErrorCode.FAIL.getCode(), errorMessage);
    }
}
