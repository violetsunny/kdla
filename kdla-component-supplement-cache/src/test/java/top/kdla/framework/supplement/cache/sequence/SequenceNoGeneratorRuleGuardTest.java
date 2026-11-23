package top.kdla.framework.supplement.cache.sequence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfg;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 回归测试：SequenceNoGenerator 非法规则串与 fillLeft null 入参守卫。
 *
 * <p>无修复则失败：缺 split.length 守卫时抛 ArrayIndexOutOfBoundsException（或被转通用消息），
 * fillLeft 收到 null 时 NPE。静态缓存按 code 隔离，每个用例用唯一 code 防污染。
 */
@ExtendWith(MockitoExtension.class)
class SequenceNoGeneratorRuleGuardTest {

    @Mock
    private CodeGeneratorCfgMapper mapper;

    @Mock
    private KRedissonRedDisLock redissonRedDisLock;

    @Mock
    private RLock lock;

    private SequenceNoGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SequenceNoGenerator();
        generator.setCodeGeneratorCfgMapper(mapper);
        generator.setRedissonClient(redissonRedDisLock);
        generator.setSequenceNoLockKey("test:seq:");
    }

    private CodeGeneratorCfg cfg(String code, String rule) {
        CodeGeneratorCfg c = new CodeGeneratorCfg();
        c.setCode(code);
        c.setRule(rule);
        return c;
    }

    @Test
    void ruleWithOneSegment_throwsBizException() {
        String code = UUID.randomUUID().toString();
        when(mapper.selectOne(any(CodeGeneratorCfg.class))).thenReturn(cfg(code, "yyyyMMdd"));

        BizException ex = assertThrows(BizException.class, () -> generator.getMaxNo(code),
                "规则仅1段须抛BizException而非数组越界");
        assertTrue(ex.getMessage().contains("规则格式不合法"), "须提示规则格式不合法");
    }

    @Test
    void ruleWithTwoSegments_throwsBizExceptionWithThreeSegmentHint() throws Exception {
        String code = UUID.randomUUID().toString();
        when(mapper.selectOne(any(CodeGeneratorCfg.class))).thenReturn(cfg(code, "H+yyyyMMdd"));
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        BizException ex = assertThrows(BizException.class, () -> generator.getMaxNo(code));
        assertTrue(ex.getMessage().contains("需要3段"),
                "规则仅2段须抛含'需要3段'的BizException，而非越界后转通用消息");
    }

    @Test
    void fillLeft_nullSource_padsToLength() throws Exception {
        Method m = SequenceNoGenerator.class.getDeclaredMethod("fillLeft", String.class, char.class, int.class);
        m.setAccessible(true);

        assertEquals("00000", m.invoke(null, null, '0', 5), "null入参须降级为空串再左填充，不得NPE");
        assertEquals("00012", m.invoke(null, "12", '0', 5));
    }
}
