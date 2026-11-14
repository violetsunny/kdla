package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：Builder/无参构造路径下 corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、handler
 * 字段均无默认值，getExecutorService() 直接传给 ThreadPoolExecutor 构造器触发自动拆箱 NPE。
 * 修复后未显式指定的参数回退到与 getDefaultExecutorService 一致的默认值。
 */
class ThreadPoolHelpBuilderDefaultTest {

    @Test
    void builderWithoutValues_getExecutorService_noUnboxingNpe() throws Exception {
        ThreadPoolHelp help = ThreadPoolHelp.builder().build();

        ExecutorService pool = help.getExecutorService();
        try {
            ThreadPoolExecutor executor = unwrap(pool);
            assertEquals(Runtime.getRuntime().availableProcessors() + 1, executor.getCorePoolSize(),
                    "corePoolSize 默认值应与 getDefaultExecutorService 一致（CPU 核数 + 1）");
            assertTrue(executor.getQueue().remainingCapacity() <= 1024,
                    "workQueue 默认值应为有界 LinkedBlockingQueue（容量 1024）");

            Future<String> future = pool.submit(() -> "ok");
            assertEquals("ok", future.get(5, TimeUnit.SECONDS), "默认参数线程池必须可正常执行任务");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void noArgConstructor_nullFields_noUnboxingNpe() {
        ExecutorService pool = new ThreadPoolHelp().getExecutorService();
        try {
            assertNotNullPool(pool);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void assertNotNullPool(ExecutorService pool) {
        assertTrue(pool != null && !pool.isShutdown(), "无参构造 + 全 null 字段不能抛 NPE");
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
