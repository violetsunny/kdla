package top.kdla.framework.supplement.cache.sequence;

import cn.hutool.core.date.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfg;

import java.lang.reflect.Field;
import java.util.Date;
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
 * 清单#10 回归测试：SequenceNoGenerator.createNo 分布式锁释放修复。
 *
 * <p>旧实现先调 lock(key) 预加锁（内部 tryLock 一次，可重入计数+1），
 * 又对返回的 RLock 再次 tryLock（计数+1），而 finally 只 unlock 一次，
 * 可重入计数残留 1，锁永不释放，只能等 leaseTime 超时。
 *
 * <p>修复后：仅通过 lock(key, TimeUnit, waitTime, leaseTime) 一次完成加锁，
 * finally 中以 isHeldByCurrentThread 守卫后 unlock 恰好一次。
 *
 * <p>无修复则失败：旧代码会调用单参 lock(String) 与 RLock.tryLock，
 * 下方两个 never() 断言不通过。
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorLockReleaseTest {

    private static final String TEST_CODE = "LOCK_RELEASE_V1_CODE";

    @Mock
    private CodeGeneratorCfgMapper mapper;

    @Mock
    private KRedissonRedDisLock redissonRedDisLock;

    @Mock
    private RLock rLock;

    private SequenceNoGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        generator = new SequenceNoGenerator();
        generator.setCodeGeneratorCfgMapper(mapper);
        generator.setRedissonClient(redissonRedDisLock);
        generator.setSequenceNoLockKey("seq:lock:");
        // 清理静态缓存，避免用例间污染
        removeStaticCacheEntry("noCacheMap");
        removeStaticCacheEntry("cfgRuleCacheMap");
    }

    private void removeStaticCacheEntry(String fieldName) throws Exception {
        Field field = SequenceNoGenerator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).remove(TEST_CODE);
    }

    @Test
    void createNo_usesSingleFourArgLock_andUnlocksOnce() throws Exception {
        CodeGeneratorCfg cfg = new CodeGeneratorCfg();
        cfg.setCode(TEST_CODE);
        cfg.setRule("KD+yyyyMMdd+8");
        cfg.setIsCache(0);
        cfg.setCacheNum(1);
        cfg.setMaxValue("");
        when(mapper.selectOne(any(CodeGeneratorCfg.class))).thenReturn(cfg);
        when(mapper.updateByIdAndRemark(any(CodeGeneratorCfg.class))).thenReturn(1);
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        String no = generator.getMaxNo(TEST_CODE);

        String today = DateUtil.format(new Date(), "yyyyMMdd");
        assertEquals("KD" + today + "00000001", no);
        // 修复验证点1：不再调用单参 lock(key) 预加锁
        verify(redissonRedDisLock, never()).lock(anyString());
        // 修复验证点2：不再对返回的 RLock 二次 tryLock
        verify(rLock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        // 修复验证点3：finally 中恰好 unlock 一次
        verify(rLock, times(1)).unlock();
    }
}
