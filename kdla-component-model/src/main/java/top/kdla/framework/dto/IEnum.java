package top.kdla.framework.dto;

/**
 * 枚举定义模板
 *
 * @author kll
 * @date 2021/10/2 10:09
 */
public interface IEnum<T> {

    /**
     * 获得编码
     * @return String
     */
    T getCode();
    /**
     * 获得描述
     * @return String
     */
    String getDesc();

}
