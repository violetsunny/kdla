package top.kdla.framework.supplement.cache.sequence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.cache.lock.RedissonRedDisLock;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfg;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 清单#附加2 回归测试：SequenceNoGenerator.getSequenceNo 规则长度段非数字时显式抛错。
 *
 * <p>旧实现 lengthStr 非合法数字(如"abc")时静默返回空串，调用方拿到空号码且根因不可追溯。
 *
 * <p>修复后：显式抛 BizException("流水号规则长度段不合法（非数字）...")。
 *
 * <p>无修复则失败：旧代码返回空串不抛异常，下方 assertThrows 不通过。
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorInvalidLengthTest {

    private static final String TEST_CODE = "INVALID_LEN_V1_CODE";

    @Mock
    private CodeGeneratorCfgMapper mapper;

    @Mock
    private RedissonRedDisLock redissonRedDisLock;

    @Mock
    private RLock rLock;

    private SequenceNoGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        generator = new SequenceNoGenerator();
        generator.setCodeGeneratorCfgMapper(mapper);
        generator.setRedissonClient(redissonRedDisLock);
        generator.setSequenceNoLockKey("seq:lock:");
        removeStaticCacheEntry("noCacheMap");
        removeStaticCacheEntry("cfgRuleCacheMap");
    }

    private void removeStaticCacheEntry(String fieldName) throws Exception {
        Field field = SequenceNoGenerator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).remove(TEST_CODE);
    }

    @Test
    void getSequenceNo_invalidLengthSegment_throwsBizException() {
        CodeGeneratorCfg cfg = new CodeGeneratorCfg();
        cfg.setCode(TEST_CODE);
        // 第三段"abc"非数字
        cfg.setRule("KD+yyyyMMdd+abc");
        cfg.setIsCache(0);
        cfg.setMaxValue("");
        when(mapper.selectOne(any(CodeGeneratorCfg.class))).thenReturn(cfg);
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> generator.getMaxNo(TEST_CODE));
        assertTrue(ex.getMessage().contains("长度段不合法"),
                "异常信息应包含'长度段不合法'，实际：" + ex.getMessage());
    }
}
