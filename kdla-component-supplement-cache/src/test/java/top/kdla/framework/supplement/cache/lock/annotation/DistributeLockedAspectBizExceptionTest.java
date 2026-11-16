package top.kdla.framework.supplement.cache.lock.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.cache.lock.RedissonLockFactory;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Verifies that DistributeLockedAspect propagates business exceptions thrown by
 * the annotated method, regardless of throwEx setting.
 *
 * Fix validated: the old code swallowed exceptions in a blanket catch(Exception).
 * After the fix, jp.proceed() exceptions propagate directly.
 */
@ExtendWith(MockitoExtension.class)
class DistributeLockedAspectBizExceptionTest {

    // Stub target class with annotation
    static class TargetService {
        @UnblockDistributeLocked(key = "testKey", throwEx = false)
        public String doWork() {
            return "ok";
        }
    }

    @Mock
    private RedissonLockFactory redissonLockFactory;

    @InjectMocks
    private DistributeLockedAspect aspect;

    @Mock
    private ProceedingJoinPoint jp;

    @Mock
    private MethodSignature signature;

    @Mock
    private RLock lock;

    private final TargetService target = new TargetService();

    @BeforeEach
    void setUp() throws Exception {
        // Wire the mocked redissonLockFactory into the aspect via reflection (it's @Autowired)
        Field field = DistributeLockedAspect.class.getDeclaredField("redissonLockFactory");
        field.setAccessible(true);
        field.set(aspect, redissonLockFactory);

        // Configure the join point to look like it targets TargetService.doWork()
        // Use lenient() for stubs that may not be invoked in every test (e.g. when lock is null)
        lenient().when(jp.getSignature()).thenReturn(signature);
        lenient().when(signature.getName()).thenReturn("doWork");
        lenient().when(signature.getParameterTypes()).thenReturn(new Class[0]);
        lenient().when(jp.getTarget()).thenReturn(target);
        lenient().when(jp.getArgs()).thenReturn(new Object[0]);
    }

    @Test
    void lockAround_bizExceptionFromProceed_propagates() throws Throwable {
        // Arrange: getLock returns our mock lock, tryLock succeeds
        when(redissonLockFactory.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(0, -1, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        // The business method throws BizException
        BizException bizEx = new BizException("business error");
        when(jp.proceed()).thenThrow(bizEx);

        // Act & Assert: BizException must propagate, not be swallowed
        assertThrows(BizException.class, () -> aspect.lockAround(jp),
                "BizException from jp.proceed() must not be swallowed by DistributeLockedAspect");

        // unlock must still be called (finally block)
        verify(lock).unlock();
    }

    @Test
    void lockAround_lockNull_throwExFalse_returnsNull() throws Throwable {
        // Arrange: getLock returns null (could not acquire lock), throwEx=false
        when(redissonLockFactory.getLock(anyString())).thenReturn(null);

        // Act: should not throw, just return null
        Object result = aspect.lockAround(jp);

        // Assert
        assert result == null : "Expected null when lock cannot be acquired and throwEx=false";
        verify(jp, never()).proceed();
    }
}
