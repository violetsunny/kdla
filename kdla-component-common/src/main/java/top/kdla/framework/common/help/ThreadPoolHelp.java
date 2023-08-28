/**
 * LY.com Inc.
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package top.kdla.framework.common.help;

/**
 * @author kanglele01
 * @version $Id: ThreadPoolComponent, v 0.1 2021/3/10 15:53 kanglele01 Exp $
 */

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.Builder;
import lombok.Setter;

import java.util.concurrent.*;

/**
 * 自定义线程池
 * <p>
 * 1，maximunPoolSize设置大小依据：
 * 有业务类型类配置，分为以下两种类型，由Runtime.getRuntime().availableProcessors()
 * 来判断服务器可使用的cpu核数在根据以下的两种类型来判断。
 * - CPU密集型
 * 该任务需要大量的运算，而且没有阻塞，需要CPU一直全速运行，
 * CPU密集任务只有在真正的多核CPU上才可能得到加速。
 * 一般计算公式：CPU核数 + 1个线程的线程池
 * - IO密集型
 * 即该任务需要大量的IO，即大量的阻塞，这种类型分以下两种情况设置
 * 1，如果IO密集型任务线程并非一直在执行任务，则应配置尽可能多的线程，如CPU核数 * 2
 * 2，参考公式：CPU核数 /1 - 阻塞系数                  阻塞系数在0.8~0.9之间
 * 比如：8核CPU：8/1 - 0.9 = 80个线程数
 * <p>
 * 2，阻塞队列种类：
 * - ArrayBlockingQueue 由数组结构组成的有界阻塞队列
 * - LinkedBlockingQueue 由链表结构组成的有界（但大小默认值为Integer.MAX_VALUE）阻塞队列
 * - PriorityBlockingQueue 支持优先级排序的无界阻塞队列
 * - DelayQueue 使用优先级队列实现的延迟无界阻塞队列
 * - SynchronousQueue 不存储元素的阻塞队列，也即单个元素的阻塞队列
 * - LinkedTransferQueue 由链表结构组成的无界阻塞队列
 * - LinkedBlockingDeque 由链表组成的双向阻塞队列
 * <p>
 * 3，拒绝策略
 * - AbortPolicy  直接抛异常阻止系统正常运行
 * - CallerRunsPolicy  由调用线程处理该任务
 * - DiscardOldestPolicy 丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
 * - DiscardPolicy  也是丢弃任务，但是不抛出异常。
 * </p>
 */
@Setter
@Builder
public class ThreadPoolHelp {
    //核心线程数
    private volatile Integer corePoolSize;
    //最大线程数
    private volatile Integer maximumPoolSize;
    //除核心线程外的线程最大空闲时间
    private volatile Long keepAliveTime;
    //空闲时间单位
    private volatile TimeUnit unit;
    //阻塞队列
    private BlockingQueue<Runnable> workQueue;
    //拒绝策略
    private RejectedExecutionHandler handler;
    //服务器的cpu核数
    private static final Integer CPUS = Runtime.getRuntime().availableProcessors();
    //默认阻塞队列
    private static final BlockingQueue<Runnable> DEFAULT_WORK_QUEUE = new LinkedBlockingQueue<Runnable>();
    //默认直接抛异常
    private static final RejectedExecutionHandler DEFAULT_HANDLER = new ThreadPoolExecutor.AbortPolicy();

    public ThreadPoolHelp() {
    }

    //自定义线程的参数
    public ThreadPoolHelp(Integer corePoolSize,
                          Integer maximumPoolSize,
                          Long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          RejectedExecutionHandler handler) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.workQueue = workQueue;
        this.handler = handler;
    }

    /**
     * 获取只有单个线程的线程池
     *
     * @return ExecutorService
     */
    public ExecutorService getSingleExecutorService() {
        ExecutorService threadPool = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                Executors.defaultThreadFactory(),
                DEFAULT_HANDLER);
        return TtlExecutors.getTtlExecutorService(threadPool);
    }

    /**
     * 自定义线程池
     *
     * @return
     */
    public ExecutorService getExecutorService() {
        return getExecutorService(this.corePoolSize,
                this.maximumPoolSize,
                this.keepAliveTime,
                this.unit,
                this.workQueue,
                this.handler);
    }

    /**
     * 获取常用的线程的线程池
     *
     * @return ExecutorService
     */
    public ExecutorService getDefaultExecutorService() {
        ExecutorService threadPool = new ThreadPoolExecutor(
                CPUS + 1,
                initMirrorIOMaxPoolSize(),
                0L,
                TimeUnit.SECONDS,
                DEFAULT_WORK_QUEUE,
                Executors.defaultThreadFactory(),
                DEFAULT_HANDLER);
        return TtlExecutors.getTtlExecutorService(threadPool);
    }

    /**
     * corePoolSize 保留在池中的线程数
     * maximumPoolSize 最大线程数
     * keepAliveTime 当线程数大于内核数时，这是多余的空闲线程将在终止之前等待新任务的最长时间。
     * unit 时间单位
     * threadFactory 新线程时使用的工厂模式
     * workQueue 等待线程队列的大小
     * handler 由于达到线程边界被阻止时使用的处理程序模式
     *
     * 实际情况下具体流程如下：
     *
     *  1）当池子大小小于corePoolSize就新建线程，并处理请求
     *
     *  2）当池子大小等于corePoolSize，把请求放入workQueue中，池子里的空闲线程就去从workQueue中取任务并处理
     *
     *  3）当workQueue放不下新入的任务时，新建线程入池，并处理请求，如果池子大小撑到了maximumPoolSize就用RejectedExecutionHandler来做拒绝处理
     *
     *  4）另外，当池子的线程数大于corePoolSize的时候，多余的线程会等待keepAliveTime长的时间，如果无请求可处理就自行销毁
     */

    /**
     * 得到线程执行对象ExecutorService
     * 固定拒绝策略为：AbortPolicy
     *
     * @return ExecutorService
     */
    public ExecutorService getExecutorService(Integer corePoolSize, Integer maximumPoolSize,
                                              Long keepAliveTime,
                                              TimeUnit unit,
                                              BlockingQueue<Runnable> workQueue,
                                              RejectedExecutionHandler handler) {
        ExecutorService threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                Executors.defaultThreadFactory(),
                handler);
        return TtlExecutors.getTtlExecutorService(threadPool);
    }


    /**
     * 当前业务为较少IO密集型的场景
     * 获取初始化好maximunPoolSize的线程池
     */
    public ExecutorService getMirrorIOExecutorService(Integer corePoolSize,
                                                      Long keepAliveTime,
                                                      TimeUnit unit,
                                                      BlockingQueue<Runnable> workQueue,
                                                      RejectedExecutionHandler handler) {
        ExecutorService threadPool = new ThreadPoolExecutor(
                corePoolSize,
                initMirrorIOMaxPoolSize(),
                keepAliveTime,
                unit,
                workQueue,
                Executors.defaultThreadFactory(),
                handler);
        return TtlExecutors.getTtlExecutorService(threadPool);
    }

    /**
     * 当前业务为较多IO密集型的场景
     * 获取初始化好maximunPoolSize的线程池
     *
     * @return ExecutorService
     */
    public ExecutorService getFullIOExecutorService(Integer corePoolSize,
                                                    Long keepAliveTime,
                                                    TimeUnit unit,
                                                    BlockingQueue<Runnable> workQueue,
                                                    RejectedExecutionHandler handler) {
        ExecutorService threadPool = new ThreadPoolExecutor(
                corePoolSize,
                initFullIOMaxPoolSize(),
                keepAliveTime,
                unit,
                workQueue,
                Executors.defaultThreadFactory(),
                handler);
        return TtlExecutors.getTtlExecutorService(threadPool);
    }

    /**
     * 当前业务为CPU计算密集型的场景
     * 获取初始化好maximunPoolSize的线程池
     *
     * @return ExecutorService
     */
    public ExecutorService getCPUExecutorService(Integer corePoolSize,
                                                 Long keepAliveTime,
                                                 TimeUnit unit,
                                                 BlockingQueue<Runnable> workQueue,
                                                 RejectedExecutionHandler handler) {
        ExecutorService threadPool = new ThreadPoolExecutor(
                corePoolSize,
                initCPUMaxPoolSize(),
                keepAliveTime,
                unit,
                workQueue,
                Executors.defaultThreadFactory(),
                handler);
        return TtlExecutors.getTtlExecutorService(threadPool);
    }

    /**
     * IO密集型
     * cpu的16倍
     *
     * @return Integer
     */
    public Integer initMirrorIOMaxPoolSize() {
        return CPUS << 4;
    }

    /**
     * IO密集型
     * cpu的20倍
     *
     * @return
     */
    public Integer initFullIOMaxPoolSize() {
        return (int) (CPUS / (1 - 0.8));
    }

    /**
     * CPU密集型
     *
     * @return Integer
     */
    public Integer initCPUMaxPoolSize() {
        return CPUS + 2;
    }
}