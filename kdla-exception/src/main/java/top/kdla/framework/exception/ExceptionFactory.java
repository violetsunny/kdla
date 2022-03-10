package top.kdla.framework.exception;

import top.kdla.framework.dto.ErrorCodeI;

/**
 * 异常工厂实现
 *
 * @author vincent.li
 * @since 2021/7/9 14:15
 */
public class ExceptionFactory {

    public static BizException buildBizException(String message) {
        return new BizException(message);
    }

    public static BizException buildBizException(String code, String message) {
        return new BizException(code, message);
    }

    public static BizException buildBizException(String code, String message, Object... args) {
        return new BizException(code, message, args);
    }

    public static BizException buildBizException(ErrorCodeI errorCodeI) {
        return new BizException(errorCodeI);
    }

    public static BizException buildBizException(ErrorCodeI errorCodeI, String message) {
        return new BizException(errorCodeI, message);
    }

    public static BizException buildBizException(String message, Throwable e) {
        return new BizException(message, e);
    }

    public static BizException buildBizException(String code, String message, Throwable e) {
        return new BizException(code, message, e);
    }

    public static SysException buildSysException(ErrorCodeI errorCodeI) {
        return new SysException(errorCodeI);
    }

    public static SysException buildSysException(ErrorCodeI errorCodeI, String message) {
        return new SysException(errorCodeI, message);
    }

    public static SysException buildSysException(String message) {
        return new SysException(message);
    }

    public static SysException buildSysException(String code, String message) {
        return new SysException(code, message);
    }

    public static SysException buildSysException(String code, String message, Object... args) {
        return new SysException(code, message, args);
    }

    public static SysException buildSysException(String message, Throwable e) {
        return new SysException(message, e);
    }

    public static SysException buildSysException(String code, String message, Throwable e) {
        return new SysException(code, message, e);
    }

}
