/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.kdla.framework.cache;

import com.google.common.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

/**
 * guava缓存
 *
 * @author kanglele
 * @version $Id: Gcache, v 0.1 2022/6/7 15:18 kanglele Exp $
 */
@Slf4j
public class KdlaGcache<T, E> {

    /**
     * 缓存并发级别
     */
    @Value("${kdla.gcache.concurrency.level:4}")
    private Integer concurrencyLevel;
    /**
     * 缓存过期时间，毫秒
     */
    @Value("${kdla.gcache.expire.after.write:1000}")
    private Long duration;
    /**
     * 缓存初始容量
     */
    @Value("${kdla.gcache.initial.capacity:10}")
    private Integer initialCapacity;
    /**
     * 缓存最大容量
     */
    @Value("${kdla.gcache.maximum.size:100}")
    private Integer maximumSize;

    public KdlaGcache() {
        super();
        init();
    }

    private LoadingCache<T, E> loadingCache;

    private void init() {
        // 缓存接口这里是LoadingCache，LoadingCache在缓存项不存在时可以自动加载缓存
        loadingCache
                // CacheBuilder的构造函数是私有的，只能通过其静态方法newBuilder()来获得CacheBuilder的实例
                = CacheBuilder.newBuilder()
                // 设置并发级别，并发级别是指可以同时写缓存的线程数
                .concurrencyLevel(concurrencyLevel)
                // 设置写缓存过期
                .expireAfterWrite(duration, TimeUnit.MILLISECONDS)
                // 设置缓存容器的初始容量
                .initialCapacity(initialCapacity)
                // 设置缓存最大容量，按照LRU最近虽少使用算法来移除缓存项
                .maximumSize(maximumSize)
                // 设置要统计缓存的命中率
                .recordStats()
                // 设置缓存的移除通知
                .removalListener(new RemovalListener<T, E>() {
                    public void onRemoval(RemovalNotification<T, E> notification) {
                        log.info("{} was removed, cause is {}", notification.getKey(), notification.getCause());
                    }
                })
                // build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
                .build(new CacheLoader<T, E>() {

                    @Override
                    public E load(T t) throws Exception {
                        log.info("cache load {}", t.toString());
                        return null;
                    }
                });
    }

    public LoadingCache<T, E> getGcache() {
        return this.loadingCache;
    }
}
