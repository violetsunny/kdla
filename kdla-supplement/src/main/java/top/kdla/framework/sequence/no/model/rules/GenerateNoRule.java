package top.kdla.framework.sequence.no.model.rules;

import lombok.Data;

/**
 * 生成编号的规则
 * @author kll
 * @date 2022/2/28
 */
@Data
public class GenerateNoRule implements Comparable<GenerateNoRule>{
    /**
     * 规则顺序
     */
    private Integer order;
    /**
     * 规则的类型
     */
    private RuleTypeEnum ruleType;
    /**
     * 规则的内容
     */
    private GenerateNoRuleContent ruleContent;

    @Override
    public int compareTo(GenerateNoRule o) {
        return this.getOrder().compareTo(o.getOrder());
    }
}
