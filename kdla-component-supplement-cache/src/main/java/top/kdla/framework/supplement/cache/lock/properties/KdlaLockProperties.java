package top.kdla.framework.supplement.cache.lock.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Distributed lock properties for KDLA.
 */
@Data
@ConfigurationProperties(prefix = "kdla.lock")
public class KdlaLockProperties {

    /**
     * Max time to wait for acquiring the lock (milliseconds).
     */
    private long waitTimeMs = 3_000L;

    /**
     * Lease time of the lock (milliseconds). -1 means no lease time.
     */
    private long leaseTimeMs = 30_000L;

    public long getWaitTimeMs() {
        return waitTimeMs;
    }

    public void setWaitTimeMs(long waitTimeMs) {
        this.waitTimeMs = waitTimeMs;
    }

    public long getLeaseTimeMs() {
        return leaseTimeMs;
    }

    public void setLeaseTimeMs(long leaseTimeMs) {
        this.leaseTimeMs = leaseTimeMs;
    }
}
