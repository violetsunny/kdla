package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：timeout 重载旧实现逐个 future.get(timeout)，总等待被放大为 N×timeout，
 * 且超时后不取消剩余任务。修复后 allOf().get(timeout) 总预算 + 超时 cancel。
 */
class MultiThreadInvokeHelpTimeoutTest {

    @Test
    void timeoutIsTotalBudget_andCancelsPendingTasks() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            AtomicInteger started = new AtomicInteger();
            // 单线程串行：3 个 600ms 任务总耗时 1800ms，timeout=1s 必须整体超时
            List<Supplier<String>> suppliers = Arrays.asList(
                    slowTask(started, 600), slowTask(started, 600), slowTask(started, 600));

            long begin = System.currentTimeMillis();
            assertThrows(TimeoutException.class,
                    () -> MultiThreadInvokeHelp.invokeGetS(suppliers, pool, 1),
                    "总耗时超 timeout 必须抛 TimeoutException，旧实现逐任务计时不会超时");
            long elapsed = System.currentTimeMillis() - begin;
            assertTrue(elapsed < 1500, "等待必须在总预算 1s 附近结束，实际 " + elapsed + "ms");

            // 超时后第 3 个任务应被 cancel，不再执行（第 2 个已在运行，无法中断，允许完成）
            Thread.sleep(900);
            assertEquals(2, started.get(), "超时后排队中的任务必须被取消，不得继续执行");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void fastTasks_withinTimeout_returnResults() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Supplier<String>> suppliers = Arrays.asList(() -> "a", () -> "b");
            List<String> results = MultiThreadInvokeHelp.invokeGetS(suppliers, pool, 5);
            assertEquals(Arrays.asList("a", "b"), results, "预算内完成的任务必须正常返回结果");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void forkJoinOverload_fastTasks_returnResults() throws Exception {
        List<Supplier<String>> suppliers = Arrays.asList(() -> "x", () -> "y");
        List<String> results = MultiThreadInvokeHelp.invokeGetS(suppliers, 5);
        assertEquals(Arrays.asList("x", "y"), results);
    }

    private static Supplier<String> slowTask(AtomicInteger started, long millis) {
        return () -> {
            started.incrementAndGet();
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "done";
        };
    }
}