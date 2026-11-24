/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import top.kdla.framework.supplement.cache.lock.properties.KRedissonConfigProperties;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kanglele
 * @version $Id: RedissonLockFactory, v 0.1 2023/2/28 14:36 kanglele Exp $
 */
@Slf4j
public class KRedissonLockFactory implements KDistributeLockFactory {

    private final static String LOCK_PATH = ":lock:";

    private final KRedissonConfigProperties redissonConfig;

    private final List<RedissonClient> redissionClientList;

    private final String lockFullPath;

    public KRedissonLockFactory(KRedissonConfigProperties redissonConfig, String appId) {
        this.redissonConfig = redissonConfig;
        this.redissionClientList = redissons();
        this.lockFullPath = appId + LOCK_PATH;
    }

    private List<RedissonClient> redissons() {
        //委托properties统一构建，消除工厂/properties双实现分叉（全模式+fail-fast+脱敏均在properties）
        return redissonConfig.redissons();
    }

    @PreDestroy
    public void destroy() {
        redissionClientList.forEach(client -> {
            try {
                client.shutdown();
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn(e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public RLock getLock(String lockKey) {
        String realLockKey = lockFullPath + lockKey;
        List<RLock> rLocks = redissionClientList.stream().map(client -> client.getLock(realLockKey)).collect(Collectors.toList());
        return new RedissonRedLock(rLocks.toArray(new RLock[rLocks.size()]));
    }

}
