package top.kdla.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * <p>
 * 是否删除枚举
 * </p>
 *
 * @author kll
 * @since 2020-12-31
 */

@Getter
@AllArgsConstructor
public enum IsDeletedEnum implements IEnum<Integer> {
    /**
     * 是
     */
    IS_DELETED_YES(1, "是"),
    /**
     * 否
     */
    IS_DELETED_NO(0, "否"),
    ;

    /**
     * 编码
     */
    private Integer code;

    /**
     * 描述
     */
    private String desc;

    /**
     * 根据code获取枚举对象
     */
    public static IsDeletedEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IsDeletedEnum codeEnum : IsDeletedEnum.values()) {
            if (code.equals(codeEnum.getCode())) {
                return codeEnum;
            }
        }
        return null;
    }

    /**
     * 根据code获取枚举(默认值为否)
     */
    public static IsDeletedEnum getDefaultByCode(Integer code) {
        return Optional.ofNullable(getByCode(code)).orElse(IS_DELETED_NO);
    }
}

