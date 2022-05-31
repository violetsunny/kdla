package top.kdla.framework.sequence.no.model.rules.random;

import lombok.Data;
import top.kdla.framework.sequence.no.model.rules.GenerateNoRuleContent;

import java.util.List;

/**
 * 序列增长型规则
 * @author kll
 * @date 2022/2/25
 */
@Data
public class RandomTypeRule implements GenerateNoRuleContent {

    private Integer size;

    private List<RandomTypeEnum> typeList;

    private List<String> customizePool;



}
