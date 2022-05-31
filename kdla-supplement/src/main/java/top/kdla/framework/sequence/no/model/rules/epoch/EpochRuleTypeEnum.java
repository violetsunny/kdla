package top.kdla.framework.sequence.no.model.rules.epoch;

/**
 * @author kll
 * @date 2022/2/28
 */
public enum EpochRuleTypeEnum {
    /**
     * 没有特殊规则，可以认为全局就一个默认的0作为纪元。所有序列号都在一个该默认值纪元下增长。
     */
    NONE,
    /**
     * 日期时间规则，每到达一个新的时间点，就是一个新的纪元。序列号只在某个时间段内增长。
     */
    DATE_TIME,
}
