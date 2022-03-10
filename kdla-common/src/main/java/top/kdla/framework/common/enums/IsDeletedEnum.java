package top.kdla.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 是否删除枚举
 * </p>
 *
 * @author vincent.li
 * @since 2020-12-31
 */

@Getter
@AllArgsConstructor
public enum IsDeletedEnum {
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
}

