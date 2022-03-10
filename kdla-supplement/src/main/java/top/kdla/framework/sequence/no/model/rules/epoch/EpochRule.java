package top.kdla.framework.sequence.no.model.rules.epoch;

import lombok.Data;

/**
 * @author hjs
 * @date 2022/2/28
 */
@Data
public class EpochRule {

    private EpochRuleTypeEnum epochRuleTypeEnum;

    private String content;

}
