package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：getDefaultExecutorService() 曾共享同一个 static 无界 LinkedBlockingQueue，
 * 多个默认池共用一条队列，任务会被其他池的工作线程消费（串扰），且队列无界存在 OOM 风险。
 * 修复后每次获取的池必须持有独立的有界队列（容量 1024）。
 */
class ThreadPoolHelpDefaultQueueTest {

    @Test
    void getDefaultExecutorService_twoPools_haveIndependentBoundedQueues() throws Exception {
        ThreadPoolHelp help = new ThreadPoolHelp();
        ExecutorService pool1 = help.getDefaultExecutorService();
        ExecutorService pool2 = help.getDefaultExecutorService();
        try {
            ThreadPoolExecutor executor1 = unwrap(pool1);
            ThreadPoolExecutor executor2 = unwrap(pool2);

            assertNotSame(executor1.getQueue(), executor2.getQueue(),
                    "两次获取的默认池必须各自持有独立队列，不能共享 static 队列");
            assertTrue(executor1.getQueue().remainingCapacity() <= 1024,
                    "默认池队列必须是有界队列（容量 1024），不能是无界 Integer.MAX_VALUE");
            assertTrue(executor2.getQueue().remainingCapacity() <= 1024,
                    "默认池队列必须是有界队列（容量 1024），不能是无界 Integer.MAX_VALUE");
        } finally {
            pool1.shutdownNow();
            pool2.shutdownNow();
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
