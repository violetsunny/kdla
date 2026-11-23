package top.kdla.framework.supplement.cache.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * C/H32 回归测试：SpringRedisHelp 锁路径修复。
 *
 * <p>unLock 旧实现用裸 key.getBytes()/value.getBytes() 绕过 RedisTemplate 序列化器，
 * 与加锁路径序列化不一致时 Lua 内 get(key)==value 永假，锁永不释放。
 * 修复后 key/value 统一走 template 序列化器。
 *
 * <p>lock 旧实现直接拆箱 setIfAbsent 返回的 Boolean，null 时 NPE。
 * 修复后 Boolean.TRUE.equals 判定，null 安全返回 false。
 *
 * <p>无修复则失败：旧 unLock 不调用序列化器（verify 失败/桩未用），旧 lock 拆箱 null 抛 NPE。
 */
@ExtendWith(MockitoExtension.class)
class SpringRedisHelpLockTest {

    @Mock
    private RedisTemplate redisTemplate;
    @Mock
    private RedisSerializer keySerializer;
    @Mock
    private RedisSerializer valueSerializer;
    @Mock
    private ValueOperations valueOperations;

    @Test
    void unLock_usesTemplateSerializers_notRawGetBytes() {
        SpringRedisHelp help = new SpringRedisHelp(redisTemplate);
        when(redisTemplate.getKeySerializer()).thenReturn(keySerializer);
        when(redisTemplate.getValueSerializer()).thenReturn(valueSerializer);
        when(keySerializer.serialize("lockKey")).thenReturn(new byte[]{1, 2, 3});
        when(valueSerializer.serialize("owner")).thenReturn(new byte[]{4, 5, 6});
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);

        boolean released = help.unLock("lockKey", "owner");

        assertTrue(released, "lua返回1表示释放成功");
        // 修复验证点：key/value 经 template 序列化器构造，而非裸 getBytes
        verify(keySerializer).serialize("lockKey");
        verify(valueSerializer).serialize("owner");
    }

    @Test
    void unLock_luaReturnsZero_returnsFalse() {
        SpringRedisHelp help = new SpringRedisHelp(redisTemplate);
        when(redisTemplate.getKeySerializer()).thenReturn(keySerializer);
        when(redisTemplate.getValueSerializer()).thenReturn(valueSerializer);
        when(keySerializer.serialize("lockKey")).thenReturn(new byte[]{1, 2, 3});
        when(valueSerializer.serialize("owner")).thenReturn(new byte[]{4, 5, 6});
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(0L);

        // lua返回0：key不存在或owner不匹配，释放失败须返回false（并log.warn可见）
        assertFalse(help.unLock("lockKey", "owner"), "lua返回0表示未释放");
    }

    @Test
    void lock_setIfAbsentNull_returnsFalse_noNpe() {
        SpringRedisHelp help = new SpringRedisHelp(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("lockKey", "owner", 1000L, TimeUnit.MILLISECONDS)).thenReturn(null);

        // 修复验证点：setIfAbsent返回null时判false而非拆箱NPE
        assertFalse(help.lock("lockKey", "owner", 1000L), "setIfAbsent返回null时应判false而非NPE");
    }
}
