package top.kdla.framework.supplement.trdcloud.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @Description: 物模型来源
 * @Author: qinkun
 * @Date: 2021/11/1 15:21
 */
@Getter
@AllArgsConstructor
public enum ModelSourceEnum {

    CUSTOM("custom", "自有");

    final String code;
    final String name;

    public static String getCodeName(String name){
        for (ModelSourceEnum e : ModelSourceEnum.values()) {
            if (e.getName().equals(name)) {
                return e.getCode();
            }
        }
        return null;
    }

    public static boolean checkCode(String code){
        return Arrays.stream(values()).anyMatch(e->e.getCode().equals(code));
    }
}
