package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：getSingleExecutorService 旧实现为 1 线程 + SynchronousQueue + AbortPolicy，
 * 第一个任务占用唯一线程时，第二个任务无处排队直接抛 RejectedExecutionException，违背"单线程串行"语义。
 * 修复后改用 LinkedBlockingQueue 排队。
 */
class ThreadPoolHelpSingleExecutorTest {

    @Test
    void singleExecutor_secondTaskQueued_notRejected() throws Exception {
        ExecutorService pool = new ThreadPoolHelp().getSingleExecutorService();
        try {
            ThreadPoolExecutor executor = unwrap(pool);
            assertTrue(executor.getQueue() instanceof java.util.concurrent.LinkedBlockingQueue,
                    "单线程池队列必须是 LinkedBlockingQueue（串行语义），不能是 SynchronousQueue");

            CountDownLatch block = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            pool.execute(() -> {
                try {
                    block.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            // 旧实现在此处抛 RejectedExecutionException（唯一线程被占用 + SynchronousQueue 无法入队）
            pool.execute(done::countDown);

            block.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS), "两个任务必须串行执行完成，第二个不能被拒绝");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 剥掉 TtlExecutors 包装（ExecutorServiceTtlWrapper 为包私有类，只能反射调用其 public unwrap()）
     */
    private static ThreadPoolExecutor unwrap(ExecutorService executorService) throws Exception {
        for (Method method : executorService.getClass().getMethods()) {
            if ("unwrap".equals(method.getName()) && method.getParameterTypes().length == 0) {
                method.setAccessible(true);
                Object delegate = method.invoke(executorService);
                if (delegate instanceof ThreadPoolExecutor) {
                    return (ThreadPoolExecutor) delegate;
                }
            }
        }
        throw new IllegalStateException("无法从 TTL 包装中取出 ThreadPoolExecutor");
    }
}
