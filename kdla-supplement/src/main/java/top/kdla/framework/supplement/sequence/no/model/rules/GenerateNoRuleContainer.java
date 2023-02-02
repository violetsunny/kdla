package top.kdla.framework.supplement.sequence.no.model.rules;

import lombok.Data;
import top.kdla.framework.supplement.sequence.no.model.rules.epoch.EpochRule;

import java.util.List;

/**
 * @author kll
 * @date 2022/2/28
 */
@Data
public class GenerateNoRuleContainer {

    /**
     * 序列号的纪元的生成规则。序列号都是在某个纪元epoch范围下，进行增长的，如果纪元epoch发生变更，那么编号在新纪元下从头开始增长。
     */
    private EpochRule epochRule;
    /**
     * 规则列表
     */
    private List<GenerateNoRule> rules;

}
