/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock;

import top.kdla.framework.exception.LockFailException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author kll
 * @version $Id: DistributeLock, v 0.1 2021/7/13 9:42 Exp $
 */
public interface DistributeLock {
    /**
     * try lock on lock key until the lock is available, and won't release the lock until call unlock()
     * @param lockKey lock name
     * @return the lock object, use to unlock.
     */
    Lock lock(String lockKey) throws LockFailException;

    /**
     * try lock on lock key wait for timeout seconds, and won't release the lock until call unlock()
     * @param lockKey lock name
     * @param timeout lock time out, unit is second
     * @return the lock object, use to unlock.
     */
    Lock lock(String lockKey, long timeout) throws LockFailException;

    /**
     * try lock on lock key wait for timeout unit, and won't release the lock until call unlock()
     * @param lockKey lock name
     * @param unit    lock time out unit
     * @param timeout lock time out, -1 means no time out
     * @return
     */
    Lock lock(String lockKey, TimeUnit unit, long timeout) throws LockFailException;

    /**
     * try lock on lock key wait for timeout unit, and won't release the lock until call unlock()
     * @param lockKey lock name
     * @param unit    lock time out unit
     * @param timeout lock time out, -1 means no time out
     * @param leaseTime lock lease time, -1 means no lease time out
     * @return
     */
    Lock lock(String lockKey, TimeUnit unit, long timeout, long leaseTime) throws LockFailException;

    /**
     * try lock on lock key wait for timeout unit, if can't lock return false; if lock success return true and will
     * lease the lock after lease time.
     * @param lockKey
     * @param unit
     * @param waitTime
     * @param leaseTime
     * @return
     */
    boolean tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) throws LockFailException;

    /**
     * unlock the lock by the lock name.
     * @param lockKey lock name
     */
    void unlock(String lockKey);

    /**
     * unlock the lock
     * @param lock lock
     */
    void unlock(Lock lock);
}
