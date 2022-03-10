package top.kdla.framework.common.help;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 多线程执行工具类 --> 函数接口转换为CompletableFuture执行
 *
 * @author kll
 * @version $Id: MultiThreadInvokeHelp.java $
 */
@Slf4j
public class MultiThreadInvokeHelp {

    /**
     * 自定义线程池执行
     * @param suppliers
     *            tasks
     * @param executor
     *            线程池
     * @return
     */
    public static <T> List<CompletableFuture<T>> invoke(List<Supplier<T>> suppliers, Executor executor) {
        return execute(suppliers, executor);
    }

    /**
     * 执行并获取结果
     * @param suppliers
     *            tasks
     * @param executor
     *            线程池
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> invokeGet(List<Supplier<T>> suppliers, Executor executor) throws Exception {
        List<T> results = new ArrayList<>();
        // 转换
        List<CompletableFuture<T>> completableFutures = execute(suppliers, executor);
        for (CompletableFuture<T> completableFuture : completableFutures) {
            // 等待获取结果
            results.add(completableFuture.get());
        }
        return results;
    }

    /**
     * 真正多线程任务执行 --> 同步等待返回值
     * @param suppliers
     *            tasks
     * @param executor
     *            线程池
     * @return
     */
    private static <T> List<CompletableFuture<T>> execute(List<Supplier<T>> suppliers, Executor executor) {
        List<CompletableFuture<T>> tasks = suppliers.stream()
            .map(supplier -> CompletableFuture.supplyAsync(supplier, executor)).collect(Collectors.toList());
        // 转换并执行汇总等待结果
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
        return tasks;
    }

    /**
     * 真正多线程任务执行 --> 无返回值
     * @param consumers
     *            tasks
     * @param executor
     *            线程池
     * @return
     */
    public static <T> List<CompletableFuture<Void>> execute(List<Consumer<T>> consumers, T t, Executor executor) {
        return consumers.stream().map(consumer -> CompletableFuture.runAsync(() -> consumer.accept(t), executor))
            .collect(Collectors.toList());
    }

    /**
     * 真正多线程任务执行 --> 无返回值，同步等待
     * @param consumers
     *            tasks
     * @param executor
     *            线程池
     * @return
     */
    public static <T> List<CompletableFuture<Void>> executeJoin(List<Consumer<T>> consumers, T t, Executor executor) {
        List<CompletableFuture<Void>> tasks =
            consumers.stream().map(consumer -> CompletableFuture.runAsync(() -> consumer.accept(t), executor)).collect(Collectors.toList());
        // 等待执行完成
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
        return tasks;
    }
}
