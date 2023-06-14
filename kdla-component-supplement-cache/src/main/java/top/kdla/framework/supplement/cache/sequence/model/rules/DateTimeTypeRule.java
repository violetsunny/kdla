package top.kdla.framework.supplement.cache.sequence.model.rules;

import lombok.Data;

/**
 * 序列增长型规则
 * @author kll
 * @date 2022/2/25
 */
@Data
public class DateTimeTypeRule implements GenerateNoRuleContent{

    private String format;

}
