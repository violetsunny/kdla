/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * caffeine
 *
 * @author kanglele
 * @version $Id: KdlaCafCache, v 0.1 2023/8/3 9:55 kanglele Exp $
 */
@Slf4j
public class KdlaCafCache<T, E> {

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

    @Getter
    private final String cacheName;

    /**
     * @param duration    超时时间，秒
     * @param maximumSize 最大容量，超过则移除
     * @param cacheName   cache名称
     */
    public KdlaCafCache(String cacheName, Long duration, Integer initialCapacity, Integer maximumSize) {
        this.cacheName = cacheName;
        this.setCafCache(duration, initialCapacity, maximumSize);
        this.init();
    }

    private Cache<T, E> loadingCache;

    private Function<T, E> loaderFunction;

    public KdlaCafCache<T, E> loaderFunction(Function<T, E> loaderFunction) {
        this.loaderFunction = loaderFunction;
        return this;
    }

    /**
     * 设置超时时间和最大容量，并初始化GCache
     *
     * @param duration    超时时间，秒
     * @param maximumSize 最大容量，超过则移除
     */
    private void setCafCache(Long duration, Integer initialCapacity, Integer maximumSize) {
        this.duration = duration != null && duration != 0 ? this.duration = duration : this.duration;
        this.initialCapacity = initialCapacity != null && initialCapacity != 0 ? this.initialCapacity = initialCapacity : this.initialCapacity;
        this.maximumSize = maximumSize != null && maximumSize != 0 ? this.maximumSize = maximumSize : this.maximumSize;
    }

    private void init() {
        // 创建一个Caffeine缓存实例
        this.loadingCache = Caffeine.newBuilder()
                // 设置缓存容器的初始容量
                .initialCapacity(this.initialCapacity)
                // 设置最大缓存数量
                .maximumSize(this.maximumSize)
                // 使用自定义的过期策略--没办法按照key设置不同的超时时间
                //.expireAfter(new CustomExpiry())
                // 设置缓存过期时间-公共过期
                .expireAfterWrite(this.duration, TimeUnit.SECONDS)
                // 设置要统计缓存的命中率
//                .recordStats()
                // 设置缓存的移除通知
                .removalListener((RemovalListener<T, E>) (k, v, removalCause) -> {
                    if (log.isWarnEnabled()) {
                        log.warn("KdlaCafCache {} was removed, cause is {}", k, removalCause);
                    }
                })
                // build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
//                .build(key -> {
//                    log.info("KdlaCafCache load {}", key);
//                    E value = this.loaderFunction == null ? null : this.loaderFunction.apply(key);
//                    // 设置key的过期时间
//                    this.loadingCache.policy().expireVariably().ifPresent(expiry -> expiry.setExpiresAfter(key, duration, TimeUnit.SECONDS));
//                    return value;
//                });
                .build();
    }

    /**
     * 获取Cache
     *
     * @return
     */
    public Cache<T, E> getCafCache() {
        return this.loadingCache;
    }

    /**
     * 放入缓存
     *
     * @param key
     * @param duration 秒
     */
    public synchronized void put(T key, long duration) {
        E value = this.loaderFunction == null ? null : this.loaderFunction.apply(key);
        assert value != null;
        // 使用Cache.policy().expireVariably()设置单个键的过期时间
        this.loadingCache.policy().expireVariably().ifPresent(expiry -> expiry.setExpiresAfter(key, duration, TimeUnit.SECONDS));
        // 将值放入缓存
        this.loadingCache.put(key, value);
    }

    /**
     * 放入缓存
     *
     * @param key
     * @param value
     * @param duration 分
     */
    public synchronized void put(T key, E value, long duration) {
        // 使用Cache.policy().expireVariably()设置单个键的过期时间
        this.loadingCache.policy().expireVariably().ifPresent(expiry -> expiry.setExpiresAfter(key, duration, TimeUnit.SECONDS));
        // 将值放入缓存
        this.loadingCache.put(key, value);
    }

    /**
     * 获取
     *
     * @param t
     * @return
     */
    public E get(T t) {
        return this.loadingCache.getIfPresent(t);
    }

    /**
     * 删除
     *
     * @param t
     */
    public void del(T t) {
        this.loadingCache.invalidate(t);
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
        return this.loadingCache.estimatedSize();
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
