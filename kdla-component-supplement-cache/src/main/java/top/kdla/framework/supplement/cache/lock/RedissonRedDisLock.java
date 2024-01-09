/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import top.kdla.framework.exception.LockFailException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author kll
 * @version $Id: RedissonRedDisLock, v 0.1 2021/7/13 9:41 Exp $
 */
@Slf4j
public class RedissonRedDisLock implements DistributeLock {
    private final DistributeLockFactory factory;

    public RedissonRedDisLock(DistributeLockFactory factory) {
        this.factory = factory;
    }

    private static final String SHADOW_KEY = "R_SHADOW:";

    @Override
    public RLock lock(String lockKey) throws LockFailException {
        return lock(lockKey, null, -1, -1);
    }

    @Override
    public RLock lock(String lockKey, long timeout) throws LockFailException {
        return lock(lockKey, TimeUnit.SECONDS, timeout);
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long timeout) throws LockFailException {
        return lock(lockKey, unit, timeout, -1);
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, long timeout, long leaseTime) throws LockFailException {
        if (log.isInfoEnabled()) {
            log.info("lock use redlock with name {} start...", lockKey);
        }

        try {
            RedissonRedLock rdfaRedLock = (RedissonRedLock) factory.getLock(lockKey);
            if (rdfaRedLock.tryLock(timeout, leaseTime, unit)) {
                return rdfaRedLock;
            } else {
                throw new LockFailException("lock use redlock with name " + lockKey + " failed.");
            }
        } catch (InterruptedException e) {
            if (log.isWarnEnabled()) {
                log.warn("lock time expired");
            }
            throw new LockFailException("lock use redlock with name " + lockKey + " failed for " + e.getMessage());
        }
    }

    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
        throw new UnsupportedOperationException("Red lock not support the try lock method.");
    }

    @Override
    public void unlock(String lockKey) {
        RedissonRedLock rdfaRedLock = (RedissonRedLock) factory.getLock(lockKey);
        this.unlock(rdfaRedLock);
    }

    @Override
    public void unlock(Lock lock) {
        if (!(lock instanceof RedissonRedLock)) {
            return;
        }
        RedissonRedLock rdfaRedLock = (RedissonRedLock) lock;
        rdfaRedLock.unlock();
    }
}

