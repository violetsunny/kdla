package top.kdla.framework.supplement.sequence.no.model.rules.random;

import com.google.common.collect.Lists;
import lombok.Getter;
import top.kdla.framework.exception.BizException;

import java.util.List;

/**
 * @author kll
 * @date 2022/2/28
 */
@Getter
public enum RandomTypeEnum {

    UPPER_CASE("UPPER_CASE", Lists.newArrayList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")),
    LOWER_CASE("LOWER_CASE", Lists.newArrayList("a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z")),
    NUMBER("NUMBER", Lists.newArrayList("0","1","2","3","4","5","6","7","8","9")),
    ;

    RandomTypeEnum(String code, List<String> pool) {
        this.code = code;
        this.pool = pool;
    }

    private String code;
    private List<String> pool;

    public static RandomTypeEnum getByCode(String code) {
        for (RandomTypeEnum e : RandomTypeEnum.values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        throw new BizException("invalid RandomTypeEnum code:" + code);
    }

}
