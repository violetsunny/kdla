package top.kdla.framework.sequence.no.model.rules;

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
