package top.kdla.framework.common.help;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
     *
     * @param suppliers tasks
     * @param executor  线程池
     * @return
     */
    public static <T> List<CompletableFuture<T>> invokeS(List<Supplier<T>> suppliers, Executor executor) {
        return executeS(suppliers, executor);
    }

    /**
     * ForkJoinPool线程池
     *
     * @param suppliers tasks
     * @return
     */
    public static <T> List<CompletableFuture<T>> invokeS(List<Supplier<T>> suppliers) {
        return executeS(suppliers);
    }

    /**
     * 执行并获取结果
     *
     * @param suppliers tasks
     * @param executor  线程池
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> invokeGetS(List<Supplier<T>> suppliers, Executor executor) throws Exception {
        List<T> results = new ArrayList<>();
        // 转换
        List<CompletableFuture<T>> completableFutures = executeS(suppliers, executor);
        for (CompletableFuture<T> completableFuture : completableFutures) {
            // 等待获取结果,get会抛出检查异常
            results.add(completableFuture.get());
        }
        return results;
    }

    /**
     * 简写，性能结果一样
     *
     * @param suppliers tasks
     * @param executor  线程池
     * @param <T>
     * @return
     */
    public static <T> List<T> invokeGetS2(List<Supplier<T>> suppliers, Executor executor) {
        List<CompletableFuture<T>> tasks = suppliers.stream().map(supplier -> CompletableFuture.supplyAsync(supplier, executor)).collect(Collectors.toList());
        return tasks.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * 执行并获取结果
     *
     * @param suppliers tasks
     *                  ForkJoinPool线程池
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> List<T> invokeGetS(List<Supplier<T>> suppliers) throws Exception {
        List<T> results = new ArrayList<>();
        // 转换
        List<CompletableFuture<T>> completableFutures = executeS(suppliers);
        for (CompletableFuture<T> completableFuture : completableFutures) {
            // 等待获取结果  get会抛出检查异常  join是运行时异常
            results.add(completableFuture.get());
        }
        return results;
    }

    /**
     * 简写，性能结果一样
     *
     * @param suppliers tasks
     *                  ForkJoinPool线程池
     * @param <T>
     * @return
     */
    public static <T> List<T> invokeGetS2(List<Supplier<T>> suppliers) {
        List<CompletableFuture<T>> tasks = suppliers.stream().map(CompletableFuture::supplyAsync).collect(Collectors.toList());
        return tasks.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * 真正多线程任务执行 --> 同步等待返回值
     *
     * @param suppliers tasks
     * @param executor  线程池
     * @return
     */
    private static <T> List<CompletableFuture<T>> executeS(List<Supplier<T>> suppliers, Executor executor) {
        //supplyAsync有返回
        List<CompletableFuture<T>> tasks = suppliers.stream().map(supplier -> CompletableFuture.supplyAsync(supplier, executor)).collect(Collectors.toList());
        // 转换并执行汇总等待结果 join是运行时异常
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
        return tasks;
    }

    /**
     * 真正多线程任务执行 --> 同步等待返回值
     *
     * @param suppliers tasks
     *                  ForkJoinPool线程池
     * @return
     */
    private static <T> List<CompletableFuture<T>> executeS(List<Supplier<T>> suppliers) {
        // supplyAsync有返回
        List<CompletableFuture<T>> tasks = suppliers.stream().map(CompletableFuture::supplyAsync).collect(Collectors.toList());
        // 转换并执行汇总等待结果 join是运行时异常
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
        return tasks;
    }

    /**
     * 真正多线程任务执行 --> 无返回值，不等待
     *
     * @param consumers tasks
     * @param executor  线程池
     * @return
     */
    public static <T> List<CompletableFuture<Void>> executeC(List<Consumer<T>> consumers, T t, Executor executor) {
        // runAsync无返回
        return consumers.stream().map(consumer -> CompletableFuture.runAsync(() -> consumer.accept(t), executor))
                .collect(Collectors.toList());
    }

    /**
     * 真正多线程任务执行 --> 无返回值，同步等待
     *
     * @param consumers tasks
     * @param executor  线程池
     * @return
     */
    public static <T> List<CompletableFuture<Void>> executeCJoin(List<Consumer<T>> consumers, T t, Executor executor) {
        List<CompletableFuture<Void>> tasks = executeC(consumers, t, executor);
        // 等待执行完成
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
        return tasks;
    }

    /**
     * 真正多线程任务执行 --> 无返回值，不等待
     *
     * @param consumers tasks
     *                  ForkJoinPool线程池
     * @return
     */
    public static <T> List<CompletableFuture<Void>> executeC(List<Consumer<T>> consumers, T t) {
        // runAsync无返回
        return consumers.stream().map(consumer -> CompletableFuture.runAsync(() -> consumer.accept(t)))
                .collect(Collectors.toList());
    }

    /**
     * 真正多线程任务执行 --> 无返回值，同步等待
     *
     * @param consumers tasks
     *                  ForkJoinPool线程池
     * @return
     */
    public static <T> List<CompletableFuture<Void>> executeCJoin(List<Consumer<T>> consumers, T t) {
        List<CompletableFuture<Void>> tasks = executeC(consumers, t);
        // 等待执行完成
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
        return tasks;
    }
}
