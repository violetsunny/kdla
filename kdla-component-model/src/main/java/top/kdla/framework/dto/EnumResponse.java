package top.kdla.framework.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class EnumResponse<T> extends BaseModel {
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
