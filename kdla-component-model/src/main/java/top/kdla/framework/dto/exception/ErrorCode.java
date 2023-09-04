package top.kdla.framework.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 * @author kll
 * @since 2021/7/12 11:15
 */
@Getter
@AllArgsConstructor
public enum ErrorCode implements ErrorCodeI {

    /**
     * 通用的业务逻辑错误默认编码
     */
    BIZ_ERROR("BIZ_ERROR", "通用的业务逻辑错误"),
    /**
     * 未知的系统错误默认编码
     */
    SYS_ERROR("SYS_ERROR", "未知的系统错误"),

    /**
     * 请求处理成功
     */
    SUCCESS("200", "请求成功"),

    /**
     * 请求处理成功 - 最终成功
     */
    FINAL_SUCCESS("201", "请求成功"),

    /**
     * 请求处理失败
     */
    FAIL("500", "请求失败"),

    /**
     * 请求错误
     */
    BAD_REQUEST("400", "请求错误"),

    /**
     * 参数错误
     */
    PARAMETER_ERROR("402", "参数错误"),

    /**
     * 无权限
     */
    UNAUTHORIZED("401", "无权限"),

    /**
     * 未知错误
     */
    UNKNOWN_ERROR("996", "未知错误"),

    /**
     * 请求实体过大
     */
    BEYOND_MAX_SIZE("413", "请求实体过大");

    /**
     * 响应码
     */
    private final String code;
    /**
     * 响应信息
     */
    private final String msg;

    /**
     * 是否成功
     * @return boolean
     */
    public boolean isSuccess(){
        return SUCCESS.getCode().equals(this.getCode());
    }
}
