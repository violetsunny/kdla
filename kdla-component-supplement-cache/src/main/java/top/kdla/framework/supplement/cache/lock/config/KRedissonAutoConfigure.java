/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import top.kdla.framework.supplement.cache.lock.KDistributeLockFactory;
import top.kdla.framework.supplement.cache.lock.KRedissonLockFactory;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;
import top.kdla.framework.supplement.cache.lock.KdlaBizDisLockService;
import top.kdla.framework.supplement.cache.lock.annotation.KDistributeLockedAspect;
import top.kdla.framework.supplement.cache.lock.properties.KRedissonConfigProperties;
import top.kdla.framework.supplement.cache.lock.properties.KdlaLockProperties;

/**
 * @author kll
 * @version $Id: RedissonAutoConfigure, v 0.1 2021/7/13 9:55 Exp $
 */
@Configuration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(name = "kdla.cache.redisson.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({KRedissonConfigProperties.class, KdlaLockProperties.class})
@Order
@Slf4j
public class KRedissonAutoConfigure {

    private static final String DEFAULT_LOCK_PREFIX = "kdla";

    @Autowired
    private KRedissonConfigProperties config;
    @Value("${app.id:}")
    private String appId;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Aspect.class)
    public KDistributeLockedAspect distributeLockedAspect(KRedissonRedDisLock redissonRedDisLock) {
        if (log.isInfoEnabled()) {
            log.info("KDistributeLockedAspect init Bean");
        }
        return new KDistributeLockedAspect(redissonRedDisLock);
    }

    @Bean
    @ConditionalOnMissingBean(KdlaBizDisLockService.class)
    public KdlaBizDisLockService disLockService(KRedissonRedDisLock redissonRedDisLock, KdlaLockProperties lockProperties) {
        if (log.isInfoEnabled()) {
            log.info("KdlaBizDisLockService init Bean");
        }
        return new KdlaBizDisLockService(redissonRedDisLock, lockProperties);
    }

    @Bean("redissonRedDisLock")
    @ConditionalOnMissingBean
    public KRedissonRedDisLock redissonRedDisLock(KDistributeLockFactory distributeLockFactory) {
        if (log.isInfoEnabled()) {
            log.info("KRedissonRedDisLock init Bean");
        }
        return new KRedissonRedDisLock(distributeLockFactory);
    }

    @Bean("distributeLockFactory")
    @ConditionalOnMissingBean(KDistributeLockFactory.class)
    public KDistributeLockFactory distributeLockFactory() {
        if (log.isInfoEnabled()) {
            log.info("KDistributeLockFactory init Bean");
        }
        String prefix;
        if (this.appId == null || "".equals(this.appId)) {
            prefix = DEFAULT_LOCK_PREFIX;
            log.warn("app.id 未配置，分布式锁前缀回落为固定值 '{}'；多实例部署请显式配置 app.id 以保证锁 key 一致", DEFAULT_LOCK_PREFIX);
        } else {
            prefix = this.appId;
        }
        return new KRedissonLockFactory(this.config, prefix);
    }

}

