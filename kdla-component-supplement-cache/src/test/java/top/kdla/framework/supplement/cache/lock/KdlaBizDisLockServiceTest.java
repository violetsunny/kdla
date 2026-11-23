package top.kdla.framework.supplement.cache.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归 biz() 丢失守卫：入参校验、持锁判定解锁、unlock 异常不掩盖业务结果。
 * 无修复则失败：无入参校验、未持锁也 unlock、裸 unlock 异常向外传播。
 */
@ExtendWith(MockitoExtension.class)
class KdlaBizDisLockServiceTest {

    @Mock
    private KRedissonRedDisLock redDisLock;

    @Mock
    private RLock rLock;

    @Test
    void blankLockKey_throwsIllegalArgument() {
        KdlaBizDisLockService service = new KdlaBizDisLockService(redDisLock);
        assertThatThrownBy(() -> service.biz(null, () -> "ok"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.biz("   ", () -> "ok"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullBizSupplier_throwsIllegalArgument() {
        KdlaBizDisLockService service = new KdlaBizDisLockService(redDisLock);
        assertThatThrownBy(() -> service.biz("k", (Supplier<String>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notHeldByCurrentThread_neverUnlocks() {
        when(redDisLock.lock("k", TimeUnit.MILLISECONDS, 3000L, 30000L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        KdlaBizDisLockService service = new KdlaBizDisLockService(redDisLock);
        String result = service.biz("k", () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(rLock, never()).unlock();
    }

    @Test
    void heldByCurrentThread_unlocksExactlyOnce() {
        when(redDisLock.lock("k", TimeUnit.MILLISECONDS, 3000L, 30000L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        KdlaBizDisLockService service = new KdlaBizDisLockService(redDisLock);
        String result = service.biz("k", () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(rLock, times(1)).unlock();
    }

    @Test
    void unlockThrows_bizStillReturnsResult() {
        when(redDisLock.lock("k", TimeUnit.MILLISECONDS, 3000L, 30000L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new RuntimeException("unlock boom")).when(rLock).unlock();

        KdlaBizDisLockService service = new KdlaBizDisLockService(redDisLock);
        String result = service.biz("k", () -> "ok");

        assertThat(result).isEqualTo("ok");
    }
}