package top.kdla.framework.dto.exception;

import top.kdla.framework.dto.IEnum;

/**
 * 响应码枚举
 * @author kll
 * @since 2021/7/12 11:15
 */
public enum ErrorCode implements ErrorCodeI, IEnum<String> {

    /**
     * 通用的业务逻辑错误默认编码
     */
    BIZ_ERROR("BIZ_ERROR", "通用的业务逻辑错误"),
    /**
     * 未知的系统错误默认编码
     */
    SYS_ERROR("007", "未知的内部系统错误"),
    /**
     * 未知错误
     */
    UNKNOWN_ERROR("996", "不能理解的业务逻辑错误"),

    /**
     * 请求处理成功
     */
    SUCCESS("200", "请求成功"),

    /**
     * 请求处理成功 - 最终成功
     */
    FINAL_SUCCESS("201", "请求成功"),

    //---系统错误
    /**
     * 请求处理失败
     */
    FAIL("2001", "请求失败"),

    /**
     * 请求错误
     */
    BAD_REQUEST("2002", "请求错误"),

    /**
     * 请求实体过大
     */
    BEYOND_MAX_SIZE("2003", "请求实体过大"),



    //---业务错误
    /**
     * 参数错误
     */
    PARAMETER_ERROR("3001", "参数错误"),

    /**
     * 无权限
     */
    UNAUTHORIZED("3002", "无权限"),

    /**
     * 唯一键重复
     */
    DUPLICATE_KEY("3003", "唯一键重复"),

    /**
     * 获取锁失败
     */
    LOCK_ERROR("3004", "获取锁失败"),

    ;

    /**
     * 响应码
     */
    private final String code;
    /**
     * 响应信息
     */
    private final String msg;

    ErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }

    @Override
    public String getDesc() {
        return this.msg;
    }

    /**
     * 是否成功
     * @return boolean
     */
    public boolean isSuccess(){
        return SUCCESS.getCode().equals(this.getCode());
    }
}
