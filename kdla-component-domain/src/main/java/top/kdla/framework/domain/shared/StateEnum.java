package top.kdla.framework.domain.shared;


import lombok.Getter;
import top.kdla.framework.dto.IEnum;

import java.util.Objects;

/**
 * 用户状态枚举
 *
 * @author haoxin
 * @date 2021-02-02
 **/
@Getter
public enum StateEnum implements ValueObject<StateEnum>, IEnum<Integer> {

    /**
     * 有效
     */
    ENABLE(0, "有效"),

    /**
     * 禁用
     */
    DISABLE(1, "禁用");


    private Integer code;

    private String desc;

    StateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    /**
     * 根据匹配code的值获取Desc
     *
     * @param code
     * @return
     */
    public static String getDescByCode(Integer code) {
        if (Objects.isNull(code)) {
            return "";
        }
        for (StateEnum s : StateEnum.values()) {
            if (code.equals(s.getCode())) {
                return s.getDesc();
            }
        }
        return "";
    }

    /**
     * 获取StateEnum
     *
     * @param code
     * @return
     */
    public static StateEnum getStateEnum(Integer code) {
        if (Objects.isNull(code)) {
            return null;
        }
        for (StateEnum s : StateEnum.values()) {
            if (code.equals(s.getCode())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public boolean sameValueAs(final StateEnum other) {
        return this.equals(other);
    }
}
