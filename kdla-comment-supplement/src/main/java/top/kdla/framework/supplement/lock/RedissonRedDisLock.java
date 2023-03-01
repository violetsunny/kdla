/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import top.kdla.framework.supplement.lock.properties.RedissonConfigProperties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * @author kll
 * @version $Id: RedissonRedDisLock, v 0.1 2021/7/13 9:41 Exp $
 */
@Deprecated
@Slf4j
public class RedissonRedDisLock implements DistributeLock {
    private RedissonConfigProperties config;
    private List<RedissonClient> redissonClientList;
    private static final String SHADOW_KEY = "R_SHADOW:";

    public RedissonRedDisLock(RedissonConfigProperties config) {
        this.config = config;
    }

    @PostConstruct
    public void init() throws Exception {
        this.redissonClientList = this.config.redissons();
    }

    @PreDestroy
    public void destroy() {
        this.redissonClientList.stream().forEach((client) -> {
            try {
                client.shutdown();
            } catch (Exception var2) {
                log.warn(var2.getMessage(), var2);
            }

        });
    }

    @Override
    public RLock lock(String lockKey) throws Exception {
        return this.lock(lockKey, (TimeUnit)null, -1L, -1L);
    }

    @Override
    public RLock lock(String lockKey, long timeout) throws Exception {
        return this.lock(lockKey, TimeUnit.SECONDS, timeout);
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long timeout) throws Exception {
        return this.lock(lockKey, unit, timeout, -1L);
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long timeout, long leaseTime) throws Exception {
        log.info("lock use redlock with name {} start...", lockKey);
        List<RLock> rlocks = (List)this.redissonClientList.stream().map((client) -> client.getLock(lockKey)).collect(Collectors.toList());
        RedissonRedLock lock = new RedissonRedLock((RLock[])rlocks.toArray(new RLock[rlocks.size()]));

        try {
            if (lock.tryLock(timeout, leaseTime, unit)) {
                log.info("lock use redlock with name {} end...", lockKey);
                return lock;
            } else {
                log.info("lock use redlock with name {} failed.", lockKey);
                throw new Exception("lock use redlock with name " + lockKey + " failed.");
            }
        } catch (InterruptedException var11) {
            log.warn("lock time expired");
            throw new Exception("lock use redlock with name " + lockKey + " failed for " + var11.getMessage());
        }
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
        throw new UnsupportedOperationException("Red lock not support the try lock method.");
    }

    @Override
    public void unlock(String lockKey) {
        throw new UnsupportedOperationException("Red lock not support the unlock by name method, please use unlock(Lock lock)");
    }

    @Override
    public void unlock(Lock lock) {
        log.info("unlock the redlock {} start...", lock);
        if (lock != null) {
            lock.unlock();
            log.info("unlock the redlock {} end...", lock);
        } else {
            log.info("lock is null, don't need to unlock");
        }

    }
}

