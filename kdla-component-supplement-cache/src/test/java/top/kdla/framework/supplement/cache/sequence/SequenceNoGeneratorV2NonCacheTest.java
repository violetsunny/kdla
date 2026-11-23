package top.kdla.framework.supplement.cache.sequence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgV2Mapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfgV2;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRule;
import top.kdla.framework.supplement.cache.sequence.model.rules.GenerateNoRuleContainer;
import top.kdla.framework.supplement.cache.sequence.model.rules.RuleTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.epoch.EpochRule;
import top.kdla.framework.supplement.cache.sequence.model.rules.epoch.EpochRuleTypeEnum;
import top.kdla.framework.supplement.cache.sequence.model.rules.sequence.SequenceTypeRule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies that SequenceNoGeneratorV2 in non-cache mode (isCache=0) generates
 * different sequence numbers on consecutive calls and persists the updated
 * max value via updateById after each call.
 *
 * Fix validated: before the fix, updateById() was not called in non-cache mode,
 * causing repeated sequence numbers across restarts.
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorV2NonCacheTest {

    private static final String TEST_CODE = "TEST_SEQ";

    @Mock
    private CodeGeneratorCfgV2Mapper mapper;

    private SequenceNoGeneratorV2 generator;

    @BeforeEach
    void setUp() throws Exception {
        generator = new SequenceNoGeneratorV2();
        generator.setCodeGeneratorCfgMapper(mapper);

        // Pre-populate ruleContainerCacheMap via reflection to avoid mapper roundtrip for rule loading
        GenerateNoRuleContainer container = buildSimpleSeqRuleContainer();
        Field ruleContainerCacheMapField = SequenceNoGeneratorV2.class
                .getDeclaredField("ruleContainerCacheMap");
        ruleContainerCacheMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, GenerateNoRuleContainer> ruleCache =
                (Map<String, GenerateNoRuleContainer>) ruleContainerCacheMapField.get(null);
        ruleCache.put(TEST_CODE, container);

        // Clear noCacheMap between test runs
        Field noCacheMapField = SequenceNoGeneratorV2.class.getDeclaredField("noCacheMap");
        noCacheMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> noCacheMap = (Map<?, ?>) noCacheMapField.get(null);
        noCacheMap.remove(TEST_CODE);
    }

    @Test
    void nonCacheMode_consecutiveCalls_produceDifferentSequenceNumbers() throws Exception {
        // Arrange: mapper returns cfg with isCache=0 on each call (simulates fresh DB state)
        when(mapper.selectByCode(TEST_CODE))
                .thenReturn(buildCfg("0"))   // first call: maxValue="0"
                .thenReturn(buildCfg("1"));  // second call: maxValue="1" (as if persisted after first)
        doNothing().when(mapper).updateById(any());

        // Access private generateSequenceNo() via reflection
        Method generateMethod = SequenceNoGeneratorV2.class
                .getDeclaredMethod("generateSequenceNo", String.class);
        generateMethod.setAccessible(true);

        // Act: two consecutive invocations
        String first = (String) generateMethod.invoke(generator, TEST_CODE);
        String second = (String) generateMethod.invoke(generator, TEST_CODE);

        // Assert: different values
        assertNotNull(first, "First sequence number must not be null");
        assertNotNull(second, "Second sequence number must not be null");
        assertNotEquals(first, second,
                "Non-cache mode must produce different sequence numbers on consecutive calls");

        // updateById must be called once per call (fix verification)
        verify(mapper, times(2)).updateById(any(CodeGeneratorCfgV2.class));
    }

    // ---- helpers ----

    private CodeGeneratorCfgV2 buildCfg(String maxValue) {
        CodeGeneratorCfgV2 cfg = new CodeGeneratorCfgV2();
        cfg.setCode(TEST_CODE);
        cfg.setIsCache(0);   // non-cache mode
        cfg.setCacheNum(1);
        cfg.setEpoch(0L);
        cfg.setMaxValue(maxValue);
        return cfg;
    }

    private GenerateNoRuleContainer buildSimpleSeqRuleContainer() {
        // A single SEQ rule: size=4, radix=10 (decimal 4-digit sequence)
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
