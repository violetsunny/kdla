package top.kdla.framework.supplement.cache.sequence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgV2Mapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfgV2;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRule;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRuleContainer;
import top.kdla.framework.supplement.cache.sequence.model.rules.RuleTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.epoch.EpochRule;
import top.kdla.framework.supplement.cache.sequence.model.rules.epoch.EpochRuleTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.sequence.SequenceTypeRule;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 清单#11 回归测试：SequenceNoGeneratorV2.createNo 分布式锁释放修复。
 *
 * <p>旧实现先调 lock(key) 预加锁（可重入计数+1），又对返回的 RLock 再次
 * tryLock（计数+1），finally 只 unlock 一次，锁永不释放。
 *
 * <p>修复后：仅通过 lock(key, TimeUnit, waitTime, leaseTime) 一次完成加锁，
 * finally 中以 isLocked + isHeldByCurrentThread 双重守卫后 unlock 恰好一次。
 *
 * <p>无修复则失败：旧代码会调用单参 lock(String) 与 RLock.tryLock，
 * 下方两个 never() 断言不通过。
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorV2LockReleaseTest {

    private static final String TEST_CODE = "LOCK_RELEASE_V2_CODE";

    @Mock
    private CodeGeneratorCfgV2Mapper mapper;

    @Mock
    private KRedissonRedDisLock redissonRedDisLock;

    @Mock
    private RLock rLock;

    private SequenceNoGeneratorV2 generator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        generator = new SequenceNoGeneratorV2();
        generator.setCodeGeneratorCfgMapper(mapper);
        generator.setRedissonClient(redissonRedDisLock);
        generator.setSequenceNoLockKey("seq:v2:lock:");

        // 反射预置规则容器缓存，避免走 mapper 加载规则 JSON
        Field ruleContainerCacheMapField = SequenceNoGeneratorV2.class.getDeclaredField("ruleContainerCacheMap");
        ruleContainerCacheMapField.setAccessible(true);
        Map<String, GenerateNoRuleContainer> ruleCache =
                (Map<String, GenerateNoRuleContainer>) ruleContainerCacheMapField.get(null);
        ruleCache.put(TEST_CODE, buildSimpleSeqRuleContainer());

        // 清理本地号段缓存，避免用例间污染
        Field noCacheMapField = SequenceNoGeneratorV2.class.getDeclaredField("noCacheMap");
        noCacheMapField.setAccessible(true);
        ((Map<?, ?>) noCacheMapField.get(null)).remove(TEST_CODE);
    }

    @Test
    void createNo_usesSingleFourArgLock_andUnlocksOnce() throws Exception {
        when(mapper.selectByCode(TEST_CODE)).thenReturn(buildCfg());
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        String no = generator.getMaxNo(TEST_CODE);

        assertEquals("0001", no);
        // 修复验证点1：不再调用单参 lock(key) 预加锁
        verify(redissonRedDisLock, never()).lock(anyString());
        // 修复验证点2：不再对返回的 RLock 二次 tryLock
        verify(rLock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        // 修复验证点3：finally 中恰好 unlock 一次
        verify(rLock, times(1)).unlock();
    }

    // ---- helpers ----

    private CodeGeneratorCfgV2 buildCfg() {
        CodeGeneratorCfgV2 cfg = new CodeGeneratorCfgV2();
        cfg.setCode(TEST_CODE);
        cfg.setIsCache(0);
        cfg.setCacheNum(1);
        cfg.setEpoch(0L);
        cfg.setMaxValue("0");
        return cfg;
    }

    private GenerateNoRuleContainer buildSimpleSeqRuleContainer() {
        // 单条 SEQ 规则：size=4, radix=10（十进制4位序列）
        SequenceTypeRule seqContent = new SequenceTypeRule();
        seqContent.setSize(4);
        seqContent.setRadix(10);

        GenerateNoRule seqRule = new GenerateNoRule();
        seqRule.setOrder(1);
        seqRule.setRuleType(RuleTypeEnum.SEQ);
        seqRule.setRuleContent(seqContent);

        EpochRule epochRule = new EpochRule();
        epochRule.setEpochRuleTypeEnum(EpochRuleTypeEnum.NONE);
        epochRule.setContent("");

        GenerateNoRuleContainer container = new GenerateNoRuleContainer();
        container.setEpochRule(epochRule);
        container.setRules(Arrays.asList(seqRule));
        return container;
    }
}
