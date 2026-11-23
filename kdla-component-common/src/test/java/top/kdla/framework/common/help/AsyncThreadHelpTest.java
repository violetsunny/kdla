package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：
 * 1. static executorService/simpleTimeLimiter 旧实现无 volatile，initExecutorService 运行期重建后
 *    其他线程可能长期读到旧引用；initExecutorService 无同步，并发重建会交错 shutdown/赋值。
 * 2. CompletableFutureBridge.prettyPrint 旧实现 long totalCost = cost.remove(taskName) 直接拆箱，
 *    重复调用（taskName 已被移除）时 NPE；未调用 allExecuted() 结算时把开始时间戳当耗时输出。
 */
class AsyncThreadHelpTest {

    @Test
    void staticPoolFields_areVolatile() throws Exception {
        Field executorField = AsyncThreadHelp.class.getDeclaredField("executorService");
        assertTrue(Modifier.isVolatile(executorField.getModifiers()), "executorService 必须为 volatile");
        Field limiterField = AsyncThreadHelp.class.getDeclaredField("simpleTimeLimiter");
        assertTrue(Modifier.isVolatile(limiterField.getModifiers()), "simpleTimeLimiter 必须为 volatile");
    }

    @Test
    void initExecutorService_isSynchronized() throws Exception {
        Method method = AsyncThreadHelp.class.getMethod("initExecutorService", int.class, int.class);
        assertTrue(Modifier.isSynchronized(method.getModifiers()), "initExecutorService 必须加同步防止并发重建交错");
    }

    @Test
    void reinit_poolStillUsable() throws Exception {
        try {
            AsyncThreadHelp.initExecutorService(1, 2);
            Future<String> future = AsyncThreadHelp.submit(() -> "ok");
            assertEquals("ok", future.get(5, TimeUnit.SECONDS), "重建后的线程池必须可正常提交任务");
        } finally {
            // 恢复静态初始化时的默认配置，避免影响其他测试
            AsyncThreadHelp.initExecutorService(1, 100);
        }
    }

    @Test
    void prettyPrint_withoutAllExecuted_noNpeOnRepeatedCall() {
        AsyncThreadHelp.CompletableFutureBridge bridge = AsyncThreadHelp.concurrentExecutor("t-npe");
        // 未 allExecuted() 结算：第一次输出截至当前耗时（旧实现输出巨大的时间戳），第二次 taskName 已移除（旧实现拆箱 NPE）
        bridge.prettyPrint();
        bridge.prettyPrint();
    }

    @Test
    void prettyPrint_afterAllExecuted_noException() {
        AsyncThreadHelp.CompletableFutureBridge bridge = AsyncThreadHelp.concurrentExecutor("t-ok");
        bridge.runAsyncWithTimeRecord(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "sub-task");
        bridge.allExecuted();
        bridge.prettyPrint();
    }
}
