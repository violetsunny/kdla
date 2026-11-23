package top.kdla.framework.supplement.cache.sequence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgV2Mapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfgV2;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRule;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRuleContainer;
import top.kdla.framework.supplement.cache.sequence.model.rules.RuleTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.epoch.EpochRule;
import top.kdla.framework.supplement.cache.sequence.model.rules.epoch.EpochRuleTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.random.RandomTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.random.RandomTypeRule;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 回归测试：SequenceNoGeneratorV2 cfg==null 与随机字符池守卫。
 *
 * <p>无修复则失败：generateSequenceNo 缺 cfg==null 守卫时 NPE；
 * generateFromRANRule 对 null customizePool 直接 addAll 抛 NPE、空池时 nextInt(0) 抛 IllegalArgumentException。
 * 异常经 createNo 包装为 BizException(cause)，按 cause 类型与消息区分根因。
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorV2RuleGuardTest {

    @Mock
    private CodeGeneratorCfgV2Mapper mapper;

    @Mock
    private KRedissonRedDisLock redissonRedDisLock;

    @Mock
    private RLock lock;

    private SequenceNoGeneratorV2 generator;

    @BeforeEach
    void setUp() {
        generator = new SequenceNoGeneratorV2();
        generator.setCodeGeneratorCfgMapper(mapper);
        generator.setRedissonClient(redissonRedDisLock);
        generator.setSequenceNoLockKey("test:seqv2:");
    }

    private void putContainer(String code, GenerateNoRuleContainer container) throws Exception {
        Field f = SequenceNoGeneratorV2.class.getDeclaredField("ruleContainerCacheMap");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, GenerateNoRuleContainer> cache = (Map<String, GenerateNoRuleContainer>) f.get(null);
        cache.put(code, container);
        Field nf = SequenceNoGeneratorV2.class.getDeclaredField("noCacheMap");
        nf.setAccessible(true);
        ((Map<?, ?>) nf.get(null)).remove(code);
    }

    private GenerateNoRuleContainer ranContainer(List<RandomTypeEnum> typeList, List<String> customizePool, int size) {
        RandomTypeRule ran = new RandomTypeRule();
        ran.setSize(size);
        ran.setTypeList(typeList);
        ran.setCustomizePool(customizePool);
        GenerateNoRule rule = new GenerateNoRule();
        rule.setOrder(1);
        rule.setRuleType(RuleTypeEnum.RAN);
        rule.setRuleContent(ran);
        EpochRule epochRule = new EpochRule();
        epochRule.setEpochRuleTypeEnum(EpochRuleTypeEnum.NONE);
        epochRule.setContent("");
        GenerateNoRuleContainer container = new GenerateNoRuleContainer();
        container.setEpochRule(epochRule);
        container.setRules(Arrays.asList(rule));
        return container;
    }

    private CodeGeneratorCfgV2 cfg(String code) {
        CodeGeneratorCfgV2 c = new CodeGeneratorCfgV2();
        c.setCode(code);
        c.setIsCache(0);
        c.setEpoch(0L);
        c.setMaxValue("0");
        return c;
    }

    private void stubLock() throws Exception {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void cfgDeletedBeforeGenerate_throwsBizExceptionWithRootCause() throws Exception {
        String code = UUID.randomUUID().toString();
        putContainer(code, ranContainer(Arrays.asList(RandomTypeEnum.NUMBER), null, 4));
        when(mapper.selectByCode(code)).thenReturn(null);
        stubLock();

        BizException ex = assertThrows(BizException.class, () -> generator.getMaxNo(code));
        assertTrue(ex.getCause() instanceof BizException,
                "cfg==null须抛BizException根因，而非NPE");
        assertTrue(ex.getCause().getMessage().contains("通过code查找配置规则失败"));
    }

    @Test
    void ranRule_emptyPool_throwsBizException() throws Exception {
        String code = UUID.randomUUID().toString();
        putContainer(code, ranContainer(Collections.emptyList(), null, 3));
        when(mapper.selectByCode(code)).thenReturn(cfg(code));
        stubLock();

        BizException ex = assertThrows(BizException.class, () -> generator.getMaxNo(code));
        assertTrue(ex.getCause() instanceof BizException, "空池须抛BizException而非NPE/nextInt(0)越界");
        assertTrue(ex.getCause().getMessage().contains("随机字符池不能为空"));
    }

    @Test
    void ranRule_nullCustomizePoolWithTypeList_generatesNormally() throws Exception {
        String code = UUID.randomUUID().toString();
        putContainer(code, ranContainer(Arrays.asList(RandomTypeEnum.NUMBER), null, 4));
        when(mapper.selectByCode(code)).thenReturn(cfg(code));
        stubLock();

        String no = generator.getMaxNo(code);

        assertTrue(no.matches("[0-9]{4}"), "customizePool为null须跳过addAll正常生成，不得NPE");
    }
}
