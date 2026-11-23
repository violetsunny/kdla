package top.kdla.framework.supplement.cache.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import top.kdla.framework.dto.exception.ErrorCodeI;
import top.kdla.framework.exception.LockFailException;
import top.kdla.framework.supplement.cache.lock.properties.KdlaLockProperties;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
public class KdlaBizDisLockService {

    private final KRedissonRedDisLock redissonRedDisLock;

    private final KdlaLockProperties lockProperties;

    public KdlaBizDisLockService(KRedissonRedDisLock redDisLock) {
        this.redissonRedDisLock = redDisLock;
        this.lockProperties = new KdlaLockProperties();
    }

    public KdlaBizDisLockService(KRedissonRedDisLock redDisLock, KdlaLockProperties lockProperties) {
        this.redissonRedDisLock = redDisLock;
        this.lockProperties = lockProperties;
    }

    /**
     * 业务执行
     *
     * @param lockKey
     * @param bizSupplier
     * @param <T>
     * @return
     */
    public <T> T biz(String lockKey, Supplier<T> bizSupplier) {
        return biz(lockKey, null, bizSupplier, null);
    }

    /**
     * 加db校验，业务执行
     *
     * @param lockKey
     * @param dbPredicate
     * @param bizSupplier
     * @param <T>
     * @return
     */
    public <T> T biz(String lockKey, Predicate<String> dbPredicate, Supplier<T> bizSupplier) {
        return biz(lockKey, dbPredicate, bizSupplier, null);
    }

    /**
     * 业务执行
     *
     * @param lockKey
     * @param bizSupplier
     * @param errCode
     * @param <T>
     * @return
     */
    public <T> T biz(String lockKey, Supplier<T> bizSupplier, ErrorCodeI errCode) {
        return biz(lockKey, null, bizSupplier, errCode);
    }

    /**
     * 加db校验，业务执行
     *
     * @param lockKey
     * @param dbPredicate
     * @param bizSupplier
     * @param errCode
     * @param <T>
     * @return
     */
    public <T> T biz(String lockKey, Predicate<String> dbPredicate, Supplier<T> bizSupplier, ErrorCodeI errCode) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            throw new IllegalArgumentException("lockKey is blank");
        }
        if (bizSupplier == null) {
            throw new IllegalArgumentException("bizSupplier is null");
        }

        RLock lock = null;
        //boolean lockRst = false;
        boolean locked = false;
        // must use try catch finnaly to lock and unlock!
        try {
            lock = redissonRedDisLock.lock(lockKey, TimeUnit.MILLISECONDS,
                    lockProperties.getWaitTimeMs(), lockProperties.getLeaseTimeMs());
            locked = true;
            // lock.tryLock()
            // 数据库锁
            if (dbPredicate == null || dbPredicate.test(lockKey)) {
                if (log.isInfoEnabled()) {
                    log.info("lock {} success!", lockKey);
                }
                // do the business
                return bizSupplier.get();
            } else {
                if (log.isInfoEnabled()) {
                    log.info("lock {} failed!", lockKey);
                }
                if (Objects.nonNull(errCode)) {
                    throw new LockFailException(errCode.getCode(), lockKey + "-重复请求");
                } else {
                    throw new LockFailException(lockKey + "-重复请求");
                }
            }
        } finally {
            if (locked && lock != null) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    log.warn("unlock failed for key: {}", lockKey, e);
                }
            }
        }
    }

}
