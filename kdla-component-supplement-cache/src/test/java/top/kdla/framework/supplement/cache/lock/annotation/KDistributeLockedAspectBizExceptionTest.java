package top.kdla.framework.supplement.cache.lock.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：KDistributeLockedAspect 须原样传播业务方法抛出的 BizException，不得吞掉。
 *
 * <p>2.x 重写把 jp.proceed() 包进 catch：throwEx=false 时吞异常返回 null，
 * throwEx=true 时转通用 Exception，丢失了 1.0.1“业务异常直接传播”的修复。
 * 修复后加锁失败仅捕获加锁调用自身，proceed 不被任何 catch 包裹，业务异常自然传播。
 *
 * <p>适配 2.x 合法变更：构造器注入 KRedissonRedDisLock、lockAround(jp,lockAction) 双参、
 * 锁经 redissonRedDisLock.lock(key,unit,timeout,leaseTime) 获取（失败抛 LockFailException）。
 *
 * <p>无修复则失败：catch(Exception) 吞掉 BizException，assertThrows 不通过。
 */
@ExtendWith(MockitoExtension.class)
class KDistributeLockedAspectBizExceptionTest {

    static class TargetService {
        @KUnblockDistributeLocked(key = "testKey", throwEx = false)
        public String doWork() {
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

    private KUnblockDistributeLocked lockAction;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new KDistributeLockedAspect(redissonRedDisLock);
        lockAction = TargetService.class.getMethod("doWork").getAnnotation(KUnblockDistributeLocked.class);
    }

    @Test
    void lockAround_bizExceptionFromProceed_propagates() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        BizException bizEx = new BizException("business error");
        when(jp.proceed()).thenThrow(bizEx);

        assertThrows(BizException.class, () -> aspect.lockAround(jp, lockAction),
                "jp.proceed() 抛出的 BizException 不得被切面吞掉");
        // finally 仍须释放锁
        verify(lock).unlock();
    }

    @Test
    void lockAround_lockNull_throwExFalse_returnsNull() throws Throwable {
        when(redissonRedDisLock.lock(anyString(), any(TimeUnit.class), anyLong(), anyLong())).thenReturn(null);

        Object result = aspect.lockAround(jp, lockAction);

        assertNull(result, "未获取到锁且 throwEx=false 时应返回 null");
        verify(jp, never()).proceed();
    }
}
