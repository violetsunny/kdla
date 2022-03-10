package top.kdla.framework.exception;

import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.dto.ErrorCodeI;

/**
 * BizException is known Exception, no need retry
 *
 * @author vincent.li
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
        this(errCode.getCode(), StringUtils.isEmpty(errMessage) ? errCode.getMsg() : errMessage);
    }

    public BizException(ErrorCodeI errCode) {
        this(errCode.getCode(), errCode.getMsg());
    }

}