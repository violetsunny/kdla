package top.kdla.framework.supplement.cache.sequence.model.rules;

import lombok.Data;

/**
 * 固定字符型规则
 * @author kll
 * @date 2022/2/25
 */
@Data
public class FixedTypeRule implements GenerateNoRuleContent{

    /**
     * 固定字符
     */
    private String content;

}
