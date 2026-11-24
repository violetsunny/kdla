package top.kdla.framework.supplement.cache.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.kdla.framework.exception.LockFailException;
import top.kdla.framework.supplement.cache.cache.SpringRedisHelp;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归 SET NX PX 锁的 tryLock(waitTime, leaseTime) 语义：
 * 自旋重试至 deadline、租约毫秒换算、token 唯一性、中断与 Redis 异常 fail-closed、
 * unlock 必须回传 token 走 Lua 比对删除。
 */
@ExtendWith(MockitoExtension.class)
class KRedisSetNxLockTest {

    private static final String APP_ID = "app";
    private static final String FULL_KEY = "app:lock:k";

    @Mock
    private SpringRedisHelp redisHelp;

    private KRedisSetNxLock newLock() {
        return new KRedisSetNxLock(redisHelp, APP_ID);
    }

    @Test
    void tryLock_defaultOverload_uses3600sLeaseAndPrefixedKey() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), eq(3_600_000L))).thenReturn(true);

        String token = newLock().tryLock("k");

        assertThat(token).isNotBlank();
        verify(redisHelp).lock(eq(FULL_KEY), eq(token), eq(3_600_000L));
    }

    @Test
    void tryLock_busyUntilDeadline_returnsNullAfterWaiting() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong())).thenReturn(false);

        long start = System.nanoTime();
        String token = newLock().tryLock("k", TimeUnit.MILLISECONDS, 250L, 3600L);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(token).isNull();
        assertThat(elapsedMs).isGreaterThanOrEqualTo(200L);
        verify(redisHelp, atLeast(2)).lock(eq(FULL_KEY), anyString(), anyLong());
    }

    @Test
    void tryLock_succeedsOnRetry_returnsToken() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong())).thenReturn(false, false, true);

        String token = newLock().tryLock("k", TimeUnit.MILLISECONDS, 2000L, 3600L);

        assertThat(token).isNotBlank();
        verify(redisHelp, times(3)).lock(eq(FULL_KEY), anyString(), anyLong());
    }

    @Test
    void tryLock_zeroWait_attemptsExactlyOnce() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong())).thenReturn(false);

        assertThat(newLock().tryLock("k", TimeUnit.SECONDS, 0L, 3600L)).isNull();
        verify(redisHelp, times(1)).lock(eq(FULL_KEY), anyString(), anyLong());
    }

    @Test
    void tryLock_negativeWait_clampedToSingleAttempt() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong())).thenReturn(false);

        assertThat(newLock().tryLock("k", TimeUnit.SECONDS, -5L, 3600L)).isNull();
        verify(redisHelp, times(1)).lock(eq(FULL_KEY), anyString(), anyLong());
    }

    @Test
    void tryLock_blankKey_throwsIllegalArgument() {
        KRedisSetNxLock lock = newLock();
        assertThatThrownBy(() -> lock.tryLock(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lock.tryLock("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryLock_nonPositiveLease_throwsIllegalArgument() {
        KRedisSetNxLock lock = newLock();
        assertThatThrownBy(() -> lock.tryLock("k", TimeUnit.SECONDS, 10L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lock.tryLock("k", TimeUnit.SECONDS, 10L, -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryLock_leaseRoundsToZeroMillis_throwsIllegalArgument() {
        assertThatThrownBy(() -> newLock().tryLock("k", TimeUnit.NANOSECONDS, 10L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryLock_nullUnit_defaultsToMilliseconds() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), eq(5000L))).thenReturn(true);

        assertThat(newLock().tryLock("k", null, 0L, 5000L)).isNotBlank();
    }

    @Test
    void twoAcquisitions_haveDistinctTokens() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong())).thenReturn(true);
        KRedisSetNxLock lock = newLock();

        String first = lock.tryLock("k");
        String second = lock.tryLock("k");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void tryLock_interrupted_returnsNullAndRestoresInterruptFlag() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong())).thenReturn(false);
        Thread.currentThread().interrupt();
        try {
            String token = newLock().tryLock("k", TimeUnit.SECONDS, 10L, 3600L);
            assertThat(token).isNull();
            assertThat(Thread.interrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void tryLock_redisError_throwsLockFailException() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong()))
                .thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> newLock().tryLock("k", TimeUnit.SECONDS, 10L, 3600L))
                .isInstanceOf(LockFailException.class);
    }

    @Test
    void unlock_delegatesSameTokenToLuaCompareAndDelete() {
        when(redisHelp.lock(eq(FULL_KEY), anyString(), anyLong())).thenReturn(true);
        when(redisHelp.unLock(eq(FULL_KEY), anyString())).thenReturn(true);
        KRedisSetNxLock lock = newLock();
        String token = lock.tryLock("k");

        assertThat(lock.unlock("k", token)).isTrue();
        verify(redisHelp).unLock(FULL_KEY, token);
    }

    @Test
    void unlock_staleToken_returnsFalse() {
        when(redisHelp.unLock(FULL_KEY, "stale")).thenReturn(false);

        assertThat(newLock().unlock("k", "stale")).isFalse();
    }

    @Test
    void unlock_nullOrEmptyToken_returnsFalseWithoutRedisCall() {
        KRedisSetNxLock lock = newLock();

        assertThat(lock.unlock("k", null)).isFalse();
        assertThat(lock.unlock("k", "")).isFalse();
        verify(redisHelp, never()).unLock(anyString(), anyString());
    }

    @Test
    void blankAppId_fallsBackToKdlaPrefix() {
        when(redisHelp.lock(eq("kdla:lock:k"), anyString(), anyLong())).thenReturn(true);

        assertThat(new KRedisSetNxLock(redisHelp, "  ").tryLock("k")).isNotBlank();
        verify(redisHelp).lock(eq("kdla:lock:k"), anyString(), anyLong());
    }
}
