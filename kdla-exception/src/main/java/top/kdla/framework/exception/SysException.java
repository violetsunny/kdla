package top.kdla.framework.exception;

import top.kdla.framework.common.utils.KdlaStringUtil;
import top.kdla.framework.dto.ErrorCodeI;

/**
 * System Exception is unexpected Exception, retry might work again
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public class SysException extends BaseException {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERR_CODE = "SYS_ERROR";

    public SysException(String message) {
        super(DEFAULT_ERR_CODE, message);
    }

    public SysException(String code, String message) {
        super(code, message);
    }

    public SysException(String code, String message, Object... args) {
        super(code, String.format(message, args));
    }

    public SysException(String message, Throwable e) {
        super(DEFAULT_ERR_CODE, message, e);
    }

    public SysException(String code, String message, Throwable e) {
        super(code, message, e);
    }

    public SysException(ErrorCodeI errCode, String errMessage) {
       this(errCode.getCode(), KdlaStringUtil.isEmpty(errMessage) ? errCode.getMsg() : errMessage);
    }

    public SysException(ErrorCodeI errCode) {
        this(errCode.getCode(), errCode.getMsg());
    }

}
