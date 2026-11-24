package top.kdla.framework.supplement.cache.lock;

import lombok.extern.slf4j.Slf4j;
import top.kdla.framework.exception.LockFailException;
import top.kdla.framework.supplement.cache.cache.SpringRedisHelp;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 RedisTemplate SET NX PX 的分布式锁，等价替代 RLock.tryLock(waitTime, leaseTime) 做多实例互斥。
 * <p>复用 {@link SpringRedisHelp#lock} 原子占位与 {@link SpringRedisHelp#unLock} Lua 比对删除，
 * 不引入 Redisson。tryLock 返回持有者 token，unlock 必须回传同一 token。
 *
 * <p><b>与 Redisson RLock 的差异（调用方必须知悉）：</b>
 * <pre>
 * 1. 不可重入：同线程同 key 二次 tryLock 会自旋至超时返回 null，不像 RLock 计数递增。
 * 2. 无看门狗：租约到期即释放。显式传 leaseTime 时 Redisson 同样不续期，此点语义一致；
 *    业务耗时超过 leaseTime 会失去互斥，须保证 leaseTime 大于业务最坏耗时。
 * 3. 无 pub/sub 唤醒：靠固定间隔轮询重试，获锁延迟最多多一个轮询间隔，Redis QPS 高于 RLock。
 * 4. 单节点：仅写一个 RedisTemplate，非 RedLock 多节点多数派。主从异步复制下故障切换可能丢锁。
 * 5. 无公平锁/无 isHeldByCurrentThread：持有关系由调用方保管 token 表达。
 * </pre>
 *
 * <p>token 唯一性是安全核心：租约过期后他人抢到锁，旧 token 经 Lua 比对不等即不删除，
 * 不会误删他人锁。故 unlock 严禁传 null 或复用旧 token。
 */
@Slf4j
public class KRedisSetNxLock {

    private static final String LOCK_PATH = ":lock:";
    private static final String DEFAULT_APP_ID = "kdla";

    /** 默认最大等待 10 秒 */
    private static final long DEFAULT_WAIT_SECONDS = 10L;
    /** 默认租约 3600 秒 */
    private static final long DEFAULT_LEASE_SECONDS = 3600L;

    /** ponytail: 固定 100ms 轮询，获锁延迟上限 100ms、等待期最多 100 次 SETNX。升级路径=Redis pub/sub 唤醒 */
    private static final long POLL_INTERVAL_MS = 100L;

    private final SpringRedisHelp redisHelp;
    private final String keyPrefix;

    public KRedisSetNxLock(SpringRedisHelp redisHelp, String appId) {
        this.redisHelp = redisHelp;
        boolean blankAppId = appId == null || appId.trim().isEmpty();
        if (blankAppId) {
            log.warn("app.id 未配置，分布式锁前缀回落为固定值 '{}'；多应用共享 Redis 请显式配置 app.id 以免锁 key 冲突", DEFAULT_APP_ID);
        }
        this.keyPrefix = (blankAppId ? DEFAULT_APP_ID : appId) + LOCK_PATH;
    }

    /**
     * 按默认 10 秒等待 / 3600 秒租约获锁。
     *
     * @param lockKey 业务锁名
     * @return 持有者 token；null 表示等待超时未获锁
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, TimeUnit.SECONDS, DEFAULT_WAIT_SECONDS, DEFAULT_LEASE_SECONDS);
    }

    /**
     * 在 waitTime 内自旋重试获锁，成功后锁自动在 leaseTime 后过期。
     *
     * @param lockKey   业务锁名，不可空白
     * @param unit      时间单位，null 按毫秒处理
     * @param waitTime  最大等待时长，0 或负数表示只尝试一次
     * @param leaseTime 租约时长，必须为正且不小于 1 毫秒
     * @return 持有者 token；null 表示未获锁（超时或被中断）
     * @throws LockFailException Redis 访问异常，fail-closed 不返回 token
     */
    public String tryLock(String lockKey, TimeUnit unit, long waitTime, long leaseTime) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            throw new IllegalArgumentException("lockKey is blank");
        }
        if (leaseTime <= 0) {
            throw new IllegalArgumentException("leaseTime must be positive");
        }
        TimeUnit actualUnit = unit == null ? TimeUnit.MILLISECONDS : unit;
        long leaseMs = actualUnit.toMillis(leaseTime);
        if (leaseMs <= 0) {
            throw new IllegalArgumentException("leaseTime must be at least 1ms");
        }
        long waitNanos = Math.max(0L, actualUnit.toNanos(waitTime));
        String fullKey = keyPrefix + lockKey;
        String token = UUID.randomUUID().toString().replace("-","");
        long deadline = System.nanoTime() + waitNanos;
        try {
            while (true) {
                if (redisHelp.lock(fullKey, token, leaseMs)) {
                    return token;
                }
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    return null;
                }
                long sleepMs = Math.min(POLL_INTERVAL_MS, remainingNanos / 1_000_000L);
                if (sleepMs <= 0) {
                    return null;
                }
                Thread.sleep(sleepMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("tryLock interrupted, key={}", lockKey);
            return null;
        } catch (RuntimeException e) {
            throw new LockFailException("tryLock failed for key " + lockKey + ": " + e.getMessage());
        }
    }

    /**
     * 释放锁。Lua 内比对 token 相等才删除，租约已过期且被他人抢占时不会误删。
     *
     * @param lockKey 业务锁名
     * @param token   tryLock 返回的持有者 token
     * @return true 确实删除了本次持有的锁；false 表示锁已不属于该 token
     */
    public boolean unlock(String lockKey, String token) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            throw new IllegalArgumentException("lockKey is blank");
        }
        if (token == null || token.isEmpty()) {
            return false;
        }
        return redisHelp.unLock(keyPrefix + lockKey, token);
    }
}
