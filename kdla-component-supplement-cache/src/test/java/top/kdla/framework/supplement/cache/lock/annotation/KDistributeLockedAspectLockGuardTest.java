package top.kdla.framework.supplement.cache.lock.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.exception.LockFailException;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：KDistributeLockedAspect 锁守卫三连修。
 *
 * <p>无修复则失败：
 * 1) finally 仅判 lock!=null 就 unlock，未持锁时 Redisson 抛 IllegalMonitorStateException；
 * 2) catch 包住 jp.proceed()，业务自抛的 LockFailException 被误归因为加锁失败；
 * 3) throwEx=true 抛裸 new Exception，丢失 BaseException 语义落到兜底 500。
 */
@ExtendWith(MockitoExtension.class)
class KDistributeLockedAspectLockGuardTest {

    static class TargetService {
        @KUnblockDistributeLocked(key = "guardKey", throwEx = false)
        public String noThrow() {
            return "ok";
        }

        @KUnblockDistributeLocked(key = "guardKey", throwEx = true)
        public String withThrow() {
            return "ok";
        }
    }

    @Mock
    private KRedissonRedDisLock redissonRedDisLock;

    @Mock
    private ProceedingJoinPoint jp;

    @Mock
    private RLock lock;

    private KDistributeLockedAspect aspect;

    private KUnblockDistributeLocked noThrowAction;

    private KUnblockDistributeLocked withThrowAction;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new KDistributeLockedAspect(redissonRedDisLock);
        noThrowAction = TargetService.class.getMethod("noThrow").getAnnotation(KUnblockDistributeLocked.class);
        withThrowAction = TargetService.class.getMethod("withThrow").getAnnotation(KUnblockDistributeLocked.class);
    }

    @Test
    void lockNull_throwExFalse_neverUnlock() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(null);

        Object result = aspect.lockAround(jp, noThrowAction);

        assertNull(result, "未获取到锁且throwEx=false应返回null");
        verify(jp, never()).proceed();
        verify(lock, never()).unlock();
    }

    @Test
    void lockNull_throwExTrue_throwsLockFailException() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(null);

        assertThrows(LockFailException.class, () -> aspect.lockAround(jp, withThrowAction),
                "throwEx=true须抛LockFailException而非裸Exception");
        verify(jp, never()).proceed();
        verify(lock, never()).unlock();
    }

    @Test
    void lockAcquireThrows_throwExTrue_throwsLockFailException_neverUnlock() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong()))
                .thenThrow(new LockFailException("acquire failed"));

        assertThrows(LockFailException.class, () -> aspect.lockAround(jp, withThrowAction));
        verify(jp, never()).proceed();
        verify(lock, never()).unlock();
    }

    @Test
    void bizLockFailException_propagatesAsIs() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        LockFailException bizEx = new LockFailException("business own lock fail");
        when(jp.proceed()).thenThrow(bizEx);

        LockFailException thrown = assertThrows(LockFailException.class, () -> aspect.lockAround(jp, noThrowAction));

        assertSame(bizEx, thrown, "业务自抛的LockFailException须原样传播，不得被误归因为加锁失败");
        verify(lock).unlock();
    }

    @Test
    void bizException_propagatesAsIs() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        BizException bizEx = new BizException("business error");
        when(jp.proceed()).thenThrow(bizEx);

        BizException thrown = assertThrows(BizException.class, () -> aspect.lockAround(jp, noThrowAction));

        assertSame(bizEx, thrown, "业务BizException须原样传播");
        verify(lock).unlock();
    }

    @Test
    void unlockThrows_businessResultStillReturned() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(jp.proceed()).thenReturn("ok");
        doThrow(new RuntimeException("unlock boom")).when(lock).unlock();

        Object result = aspect.lockAround(jp, noThrowAction);

        assertEquals("ok", result, "unlock失败仅告警，不得炸掉调用方");
        verify(lock).unlock();
    }

    @Test
    void notHeldByCurrentThread_neverUnlock() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(false);
        when(jp.proceed()).thenReturn("ok");

        Object result = aspect.lockAround(jp, noThrowAction);

        assertEquals("ok", result);
        verify(lock, never()).unlock();
    }
}
