package top.kdla.framework.supplement.cache.sequence;

import cn.hutool.core.date.DateUtil;
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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 清单#13 回归测试：SequenceNoGenerator.generate 序列溢出时拒绝生成。
 *
 * <p>旧实现溢出时 break 跳出循环，随后照常 setMaxValue 并持久化，
 * 导致 maxValue 停留在溢出前的截断值，后续号码从头重复发放。
 *
 * <p>修复后：溢出直接抛 BizException，绝不持久化 maxValue。
 *
 * <p>无修复则失败：旧代码 break 后仍会调用 updateByIdAndRemark，
 * 下方 never() 断言不通过。
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorOverflowRejectTest {

    private static final String TEST_CODE = "OVERFLOW_REJECT_V1_CODE";

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
    void generate_overflow_throwsBizException_andNeverPersists() throws Exception {
        String today = DateUtil.format(new Date(), "yyyyMMdd");
        CodeGeneratorCfg cfg = new CodeGeneratorCfg();
        cfg.setCode(TEST_CODE);
        // 序列位宽2位（最大99），当前已到96；cacheNum=5 需生成 97,98,99,100(溢出),101
        cfg.setRule("KD+yyyyMMdd+2");
        cfg.setIsCache(1);
        cfg.setCacheNum(5);
        cfg.setMaxValue("KD" + today + "96");
        when(mapper.selectOne(any(CodeGeneratorCfg.class))).thenReturn(cfg);
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // 修复验证点1：溢出时抛 BizException，不返回截断号码
        assertThrows(BizException.class, () -> generator.getMaxNo(TEST_CODE));
        // 修复验证点2：溢出后绝不持久化 maxValue（旧实现 break 后照常 update）
        verify(mapper, never()).updateByIdAndRemark(any(CodeGeneratorCfg.class));
        // 异常路径下锁仍被正常释放
        verify(rLock, times(1)).unlock();
    }
}
