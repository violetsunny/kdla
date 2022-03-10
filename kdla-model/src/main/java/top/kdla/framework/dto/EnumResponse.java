package top.kdla.framework.dto;

import lombok.Data;

@Data
public class EnumResponse<T> extends BaseDto {
    /**
     * 枚举的code
     */
    private T code;
    /**
     * 枚举的描述
     */
    private String desc;
    /**
     * 符号
     */
    private String mark;
}
