package top.kdla.framework.supplement.cache.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import top.kdla.framework.dto.ErrorCodeI;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.exception.LockFailException;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
public class KdlaBizDisLockService {

    private final RedissonRedDisLock redDisLock;

    public KdlaBizDisLockService(RedissonRedDisLock redDisLock) {
        this.redDisLock = redDisLock;
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
        RLock lock = null;
        boolean lockRst = false;
        // must use try catch finnaly to lock and unlock!
        try {
            lock = redDisLock.lock(lockKey);
            // lock.lock()
            lockRst = lock.tryLock();
            //数据库锁
            if (lockRst && (dbPredicate == null || dbPredicate.test(lockKey))) {
                log.info("lock {} success!", lockKey);
                // do the business
                return bizSupplier.get();
            } else {
                log.info("lock {} failed!", lockKey);
                if (Objects.nonNull(errCode)) {
                    throw new LockFailException(errCode);
                } else {
                    throw new LockFailException("重复请求");
                }
            }
        } finally {
            if (lockRst) {
                lock.unlock();
            }
        }
    }

}
