/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.RemovalListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
    private Integer concurrencyLevel;
    /**
     * 缓存过期时间，秒
     */
    private Long duration;
    /**
     * 缓存初始容量
     */
    private Integer initialCapacity;
    /**
     * 缓存最大容量
     */
    private Integer maximumSize;

    /**
     * @param duration    超时时间，秒
     * @param maximumSize 最大容量，超过则移除
     */
    public KdlaGcache(Long duration, Integer initialCapacity, Integer maximumSize, Integer concurrencyLevel) {
        this.setGCache(duration, initialCapacity, maximumSize, concurrencyLevel);
        this.init();
    }

    private Cache<T, CacheValue<E>> loadingCache;

    private Function<T, E> loaderFunction;

    public KdlaGcache<T, E> loaderFunction(Function<T, E> loaderFunction){
        this.loaderFunction = loaderFunction;
        return this;
    }

    /**
     * 设置超时时间和最大容量，并初始化GCache
     *
     * @param duration    超时时间，秒
     * @param maximumSize 最大容量，超过则移除
     */
    private void setGCache(Long duration, Integer initialCapacity, Integer maximumSize, Integer concurrencyLevel) {
        this.duration = duration != null && duration != 0 ? this.duration = duration : this.duration;
        this.initialCapacity = initialCapacity != null && initialCapacity != 0 ? this.initialCapacity = initialCapacity : this.initialCapacity;
        this.maximumSize = maximumSize != null && maximumSize != 0 ? this.maximumSize = maximumSize : this.maximumSize;
        this.concurrencyLevel = concurrencyLevel != null && concurrencyLevel != 0 ? this.concurrencyLevel = concurrencyLevel : this.concurrencyLevel;
    }

    /**
     * 初始化GCache
     */
    private void init() {
        // 缓存接口这里是LoadingCache，LoadingCache在缓存项不存在时可以自动加载缓存
        this.loadingCache
                // CacheBuilder的构造函数是私有的，只能通过其静态方法newBuilder()来获得CacheBuilder的实例
                = CacheBuilder.newBuilder()
                // 设置并发级别，并发级别是指可以同时写缓存的线程数
                .concurrencyLevel(this.concurrencyLevel)
                // 设置写缓存过期
                .expireAfterWrite(this.duration, TimeUnit.SECONDS)
                // 设置缓存容器的初始容量
                .initialCapacity(this.initialCapacity)
                // 设置缓存最大容量，按照LRU最近虽少使用算法来移除缓存项
                .maximumSize(this.maximumSize)
                // 设置要统计缓存的命中率
                .recordStats()
                // 设置缓存的移除通知
                .removalListener((RemovalListener<T, CacheValue<E>>) notification -> log.info("KdlaGcache {} was removed, cause is {}", notification.getKey(), notification.getCause()))
                // build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
                .build();
    }

    /**
     * 获取Cache
     *
     * @return
     */
    public Cache<T, CacheValue<E>> getGCache() {
        return this.loadingCache;
    }

    /**
     * cache value
     */
    @Data
    private static class CacheValue<E> {
        //cache value
        private E value;
        //秒
        private Long expiryTime;

        CacheValue(E value, Long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }

    /**
     * 放入缓存
     *
     * @param key
     * @param duration 秒
     */
    public synchronized void put(T key, Long duration) {
        E value = this.loaderFunction == null ? null : this.loaderFunction.apply(key);
        assert value != null;
        long expiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(duration);
        this.loadingCache.put(key, new CacheValue<>(value, expiryTime));
    }

    /**
     * 获取
     *
     * @param key
     */
    public synchronized E get(T key) {
        CacheValue<E> cacheValue = this.loadingCache.getIfPresent(key);
        if (cacheValue == null || System.currentTimeMillis() > cacheValue.getExpiryTime()) {
            this.loadingCache.invalidate(key);
            return null;
        }
        return cacheValue.getValue();
    }


    /**
     * 删除
     *
     * @param key
     */
    public void del(T key) {
        this.loadingCache.invalidate(key);
    }

    /**
     * 清除
     */
    public void clear() {
        this.loadingCache.invalidateAll();
    }

    /**
     * 缓存大小
     *
     * @return
     */
    public long size() {
        return this.loadingCache.size();
    }

    /**
     * 统计信息
     *
     * @return
     */
    public CacheStats getStats() {
        return this.loadingCache.stats();
    }

}
