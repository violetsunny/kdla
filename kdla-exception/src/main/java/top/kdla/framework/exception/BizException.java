package top.kdla.framework.exception;

import top.kdla.framework.common.utils.KdlaStringUtils;
import top.kdla.framework.dto.ErrorCodeI;

/**
 * BizException is known Exception, no need retry
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public class BizException extends BaseException {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERR_CODE = "BIZ_ERROR";

    public BizException(String message) {
        super(DEFAULT_ERR_CODE, message);
    }

    public BizException(String code, String message) {
        super(code, message);
    }

    public BizException(String code, String message, Object... args) {
        super(code, String.format(message, args));
    }

    public BizException(String message, Throwable e) {
        super(DEFAULT_ERR_CODE, message, e);
    }

    public BizException(String code, String message, Throwable e) {
        super(code, message, e);
    }

    public BizException(ErrorCodeI errCode, String errMessage) {
        this(errCode.getCode(), KdlaStringUtils.isEmpty(errMessage) ? errCode.getMsg() : errMessage);
    }

    public BizException(ErrorCodeI errCode) {
        this(errCode.getCode(), errCode.getMsg());
    }

}