package top.kdla.framework.dto;

/**
 * 错误编码描述声明
 * @author vincent.li
 * @since 2021/7/12
 */
public interface ErrorCodeI {

    /**
     * 返回响应码
     * @return String
     */
    String getCode();

    /**
     * 返回响应码描述信息
     * @return String
     */
    String getMsg();
}
