package top.kdla.framework.supplement.cache.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.RedissonRedLock;
import top.kdla.framework.exception.LockFailException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归 KRedissonRedDisLock 丢失修复：null TimeUnit 防御、中断恢复、tryLock 异常类型、unlock 平铺。
 * 无修复则失败：单参 lock 传 null TimeUnit、中断不置标志、tryLock 抛 LockFailException。
 */
@ExtendWith(MockitoExtension.class)
class KRedissonRedDisLockTest {

    @Mock
    private KDistributeLockFactory factory;

    @Mock
    private RedissonRedLock redLock;

    @Test
    void lockSingleArg_defaultsToMilliseconds() throws Exception {
        when(factory.getLock("k")).thenReturn(redLock);
        when(redLock.tryLock(-1L, -1L, TimeUnit.MILLISECONDS)).thenReturn(true);

        KRedissonRedDisLock disLock = new KRedissonRedDisLock(factory);
        disLock.lock("k");

        verify(redLock).tryLock(-1L, -1L, TimeUnit.MILLISECONDS);
    }

    @Test
    void lockNullUnit_defaultsToMilliseconds() throws Exception {
        when(factory.getLock("k")).thenReturn(redLock);
        when(redLock.tryLock(-1L, -1L, TimeUnit.MILLISECONDS)).thenReturn(true);

        KRedissonRedDisLock disLock = new KRedissonRedDisLock(factory);
        disLock.lock("k", null, -1L, -1L);

        verify(redLock).tryLock(-1L, -1L, TimeUnit.MILLISECONDS);
    }

    @Test
    void lockInterrupted_restoresInterruptFlagAndThrowsLockFail() throws Exception {
        when(factory.getLock("k")).thenReturn(redLock);
        when(redLock.tryLock(-1L, -1L, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("boom"));

        KRedissonRedDisLock disLock = new KRedissonRedDisLock(factory);
        try {
            assertThatThrownBy(() -> disLock.lock("k", TimeUnit.MILLISECONDS, -1L, -1L))
                    .isInstanceOf(LockFailException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void tryLockOverload_throwsUnsupportedOperation() {
        KRedissonRedDisLock disLock = new KRedissonRedDisLock(factory);
        assertThatThrownBy(() -> disLock.tryLock("k", TimeUnit.MILLISECONDS, 1L, 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void unlockNonRedLock_returnsWithoutThrowing() {
        KRedissonRedDisLock disLock = new KRedissonRedDisLock(factory);
        Lock other = mock(Lock.class);
        assertThatCode(() -> disLock.unlock(other)).doesNotThrowAnyException();
        assertThatCode(() -> disLock.unlock((Lock) null)).doesNotThrowAnyException();
    }

    @Test
    void unlockRedLock_delegatesOnce() {
        KRedissonRedDisLock disLock = new KRedissonRedDisLock(factory);
        disLock.unlock(redLock);
        verify(redLock).unlock();
    }
}