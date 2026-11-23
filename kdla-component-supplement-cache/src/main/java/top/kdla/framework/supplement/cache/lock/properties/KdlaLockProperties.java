package top.kdla.framework.supplement.cache.lock.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁等待/租约时长配置（kdla.lock.*）。
 */
@Data
@ConfigurationProperties(prefix = "kdla.lock")
public class KdlaLockProperties {

    /**
     * 获取锁的最大等待时长（毫秒）。
     */
    private long waitTimeMs = 3_000L;

    /**
     * 锁租约时长（毫秒），-1 表示不设租约（看门狗自动续期）。
     */
    private long leaseTimeMs = 30_000L;
}