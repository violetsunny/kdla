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
import static org.mockito.Mockito.when;

/**
 * 清单#附加1 回归测试：SequenceNoGenerator.generate 日期翻越当天首个号码不被误判溢出。
 *
 * <p>旧实现溢出检查在日期翻越重置之前：翻越当天用上一周期最大编号(如99)+1=100，
 * 超过2位位宽即误抛"流水号已超过最大限度"，导致跨天首个号码无法生成。
 *
 * <p>修复后：先判断日期(key)翻越并重置 sequenceNo=1，再做溢出检查，
 * 翻越当天正确返回 "KD"+today+"01"。
 *
 * <p>无修复则失败：旧代码在翻越当天抛 BizException，下方 assertEquals 不通过。
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorRolloverTest {

    private static final String TEST_CODE = "ROLLOVER_V1_CODE";

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
        removeStaticCacheEntry("noCacheMap");
        removeStaticCacheEntry("cfgRuleCacheMap");
    }

    private void removeStaticCacheEntry(String fieldName) throws Exception {
        Field field = SequenceNoGenerator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).remove(TEST_CODE);
    }

    @Test
    void generate_dateRollover_resetsToOne_notOverflow() throws Exception {
        String today = DateUtil.format(new Date(), "yyyyMMdd");
        String yesterday = DateUtil.format(DateUtil.offsetDay(new Date(), -1), "yyyyMMdd");
        CodeGeneratorCfg cfg = new CodeGeneratorCfg();
        cfg.setCode(TEST_CODE);
        // 位宽2位，上一周期(昨天)已达最大99；翻越当天应重置为01而非误判溢出
        cfg.setRule("KD+yyyyMMdd+2");
        cfg.setIsCache(0);
        cfg.setMaxValue("KD" + yesterday + "99");
        when(mapper.selectOne(any(CodeGeneratorCfg.class))).thenReturn(cfg);
        when(mapper.updateByIdAndRemark(any(CodeGeneratorCfg.class))).thenReturn(1);
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // 修复验证点：翻越当天首个号码为 "KD"+today+"01"，不抛溢出异常
        assertEquals("KD" + today + "01", generator.getMaxNo(TEST_CODE));
    }
}
