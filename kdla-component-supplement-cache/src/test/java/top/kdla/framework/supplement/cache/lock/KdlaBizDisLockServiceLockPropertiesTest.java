package top.kdla.framework.supplement.cache.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import top.kdla.framework.supplement.cache.lock.properties.KdlaLockProperties;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归 D：恢复 KdlaLockProperties 后 biz() 走有界 tryLock(waitTimeMs,leaseTimeMs)，而非旧单参 lock(lockKey)。
 * 无修复则失败：旧实现调用单参 lock，下方四参 verify 不通过。
 */
@ExtendWith(MockitoExtension.class)
class KdlaBizDisLockServiceLockPropertiesTest {

    @Mock
    private KRedissonRedDisLock redDisLock;

    @Mock
    private RLock rLock;

    @Test
    void biz_usesConfiguredWaitAndLease() {
        KdlaLockProperties props = new KdlaLockProperties();
        props.setWaitTimeMs(1234L);
        props.setLeaseTimeMs(5678L);
        when(redDisLock.lock("k", TimeUnit.MILLISECONDS, 1234L, 5678L)).thenReturn(rLock);

        KdlaBizDisLockService service = new KdlaBizDisLockService(redDisLock, props);
        String result = service.biz("k", () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(redDisLock).lock("k", TimeUnit.MILLISECONDS, 1234L, 5678L);
    }

    @Test
    void defaultConstructor_usesDefaultWaitAndLease() {
        when(redDisLock.lock("k", TimeUnit.MILLISECONDS, 3000L, 30000L)).thenReturn(rLock);

        KdlaBizDisLockService service = new KdlaBizDisLockService(redDisLock);
        service.biz("k", () -> "ok");

        verify(redDisLock).lock("k", TimeUnit.MILLISECONDS, 3000L, 30000L);
    }
}