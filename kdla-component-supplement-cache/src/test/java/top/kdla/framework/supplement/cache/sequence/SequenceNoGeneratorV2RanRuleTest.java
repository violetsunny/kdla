package top.kdla.framework.supplement.cache.sequence;

import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoInfoContext;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRule;
import top.kdla.framework.supplement.cache.sequence.model.rules.random.RandomTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.random.RandomTypeRule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全回归：RAN 规则随机段由 java.util.Random（48-bit LCG 可预测）改为 SecureRandom。
 * 不可预测性无法从输出单元断言，本测试守护替换后行为不变：长度、字符池约束、空池拒绝。
 */
class SequenceNoGeneratorV2RanRuleTest {

    private String invokeRan(RandomTypeRule content) throws Exception {
        GenerateNoRule rule = new GenerateNoRule();
        rule.setRuleContent(content);
        Method m = SequenceNoGeneratorV2.class.getDeclaredMethod(
                "generateFromRANRule", GenerateNoInfoContext.class, GenerateNoRule.class);
        m.setAccessible(true);
        try {
            return (String) m.invoke(new SequenceNoGeneratorV2(), null, rule);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    @Test
    void ranRule_numberPool_sizeAndCharset() throws Exception {
        RandomTypeRule content = new RandomTypeRule();
        content.setSize(64);
        content.setTypeList(Collections.singletonList(RandomTypeEnum.NUMBER));
        String result = invokeRan(content);
        assertEquals(64, result.length());
        assertTrue(result.matches("[0-9]{64}"), "随机段必须全部来自数字池");
    }

    @Test
    void ranRule_customizePool_onlyPoolChars() throws Exception {
        RandomTypeRule content = new RandomTypeRule();
        content.setSize(32);
        content.setTypeList(Collections.emptyList());
        content.setCustomizePool(java.util.Arrays.asList("X", "Y"));
        String result = invokeRan(content);
        assertEquals(32, result.length());
        assertTrue(result.matches("[XY]{32}"), "随机段必须全部来自自定义池");
    }

    @Test
    void ranRule_emptyPool_throwsBizException() {
        RandomTypeRule content = new RandomTypeRule();
        content.setSize(8);
        content.setTypeList(Collections.emptyList());
        assertThrows(BizException.class, () -> invokeRan(content), "空字符池必须拒绝");
    }
}