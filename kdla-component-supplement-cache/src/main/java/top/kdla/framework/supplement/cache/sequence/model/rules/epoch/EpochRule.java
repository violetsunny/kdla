package top.kdla.framework.supplement.cache.sequence.model.rules.epoch;

import lombok.Data;

/**
 * @author kll
 * @date 2022/2/28
 */
@Data
public class EpochRule {

    private EpochRuleTypeEnum epochRuleTypeEnum;

    private String content;

}
