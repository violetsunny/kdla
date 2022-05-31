package top.kdla.framework.sequence.no.model.rules;

import lombok.Getter;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.sequence.no.model.rules.random.RandomTypeRule;
import top.kdla.framework.sequence.no.model.rules.sequence.SequenceTypeRule;

/**
 * 序列编号的类型
 *
 * @author kll
 * @date 2022/2/25
 */
@Getter
public enum RuleTypeEnum {

    SEQ("SEQ", "序列增长类型", SequenceTypeRule.class),
    FIX("FIX", "固定字符类型", FixedTypeRule.class),
    DT("DT", "日期时间类型", DateTimeTypeRule.class),
    RAN("RAN", "随机字符串类型", RandomTypeRule.class),
    ;


    RuleTypeEnum(String code, String desc, Class<? extends GenerateNoRuleContent> ruleClass) {
        this.code = code;
        this.desc = desc;
        this.ruleClass = ruleClass;
    }

    private String code;
    private String desc;
    private Class ruleClass;

    public static RuleTypeEnum getByCode(String code) {
        for (RuleTypeEnum e : RuleTypeEnum.values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        throw new BizException("invalid SequenceTypeEnum code:" + code);
    }
}
