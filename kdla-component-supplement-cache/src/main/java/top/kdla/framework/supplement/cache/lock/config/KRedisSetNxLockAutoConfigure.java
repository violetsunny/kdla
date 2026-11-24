package top.kdla.framework.supplement.cache.lock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import top.kdla.framework.supplement.cache.cache.SpringRedisHelp;
import top.kdla.framework.supplement.cache.cache.config.CacheAutoConfigure;
import top.kdla.framework.supplement.cache.lock.KRedisSetNxLock;

/**
 * SET NX PX 分布式锁装配，不依赖 Redisson。
 * 前置条件：SpringRedisHelp 存在（由 kdla.cache.enabled=true 的 CacheAutoConfigure 提供）。
 * 默认开启，可用 kdla.cache.setnx-lock.enabled=false 关闭；下游自定义同类型 Bean 时让位。
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnBean(SpringRedisHelp.class)
@AutoConfigureAfter(CacheAutoConfigure.class)
@ConditionalOnProperty(name = "kdla.cache.setnx-lock.enabled", havingValue = "true", matchIfMissing = true)
public class KRedisSetNxLockAutoConfigure {

    @Value("${app.id:}")
    private String appId;

    @Bean
    @ConditionalOnMissingBean(KRedisSetNxLock.class)
    public KRedisSetNxLock kRedisSetNxLock(SpringRedisHelp springRedisHelp) {
        return new KRedisSetNxLock(springRedisHelp, appId);
    }
}
