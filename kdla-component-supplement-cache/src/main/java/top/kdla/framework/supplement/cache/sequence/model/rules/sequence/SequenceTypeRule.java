package top.kdla.framework.supplement.cache.sequence.model.rules.sequence;

import lombok.Data;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRuleContent;

/**
 * 序列增长型规则
 * @author kll
 * @date 2022/2/25
 */
@Data
public class SequenceTypeRule implements GenerateNoRuleContent {

    /**
     * 序列的大小
     */
    private Integer size;
    /**
     * 序列的进制,默认十进制
     */
    private Integer radix = 10;



}
