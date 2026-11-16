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
import top.kdla.framework.supplement.cache.lock.KdlaBizDisLockService;
import top.kdla.framework.supplement.cache.lock.DistributeLockFactory;
import top.kdla.framework.supplement.cache.lock.RedissonLockFactory;
import top.kdla.framework.supplement.cache.lock.RedissonRedDisLock;
import top.kdla.framework.supplement.cache.lock.annotation.DistributeLockedAspect;
import top.kdla.framework.supplement.cache.lock.properties.KdlaLockProperties;
import top.kdla.framework.supplement.cache.lock.properties.RedissonConfigProperties;

/**
 * Redisson 分布式锁自动配置。
 * <p>
 * 默认开启（T 现状已注册生效，B5/S1 裁定保持），宿主可配置 {@code kdla.cache.redisson.enabled=false} 关闭，
 * 且 classpath 必须存在 redisson（本库中为 provided scope）。
 *
 * @author kll
 * @version $Id: RedissonAutoConfigure, v 0.1 2021/7/13 9:55 Exp $
 */
@Slf4j
@Configuration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(name = "kdla.cache.redisson.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({RedissonConfigProperties.class, KdlaLockProperties.class})
public class RedissonAutoConfigure {

    private static final String DEFAULT_LOCK_PREFIX = "kdla";

    @Autowired
    private RedissonConfigProperties config;
    @Value("${app.id:}")
    private String appId;

    @Bean("disLockService")
    @ConditionalOnMissingBean(KdlaBizDisLockService.class)
    public KdlaBizDisLockService disLockService(RedissonRedDisLock redissonRedDisLock, KdlaLockProperties lockProperties){
        return new KdlaBizDisLockService(redissonRedDisLock, lockProperties);
    }

    @Bean("redissonRedDisLock")
    @ConditionalOnMissingBean
    public RedissonRedDisLock redissonRedDisLock(DistributeLockFactory distributeLockFactory) {
        return new RedissonRedDisLock(distributeLockFactory);
    }

    @Bean("distributeLockFactory")
    @ConditionalOnMissingBean(DistributeLockFactory.class)
    public DistributeLockFactory distributeLockFactory() {
        String prefix;
        if (this.appId == null || "".equals(this.appId)) {
            prefix = DEFAULT_LOCK_PREFIX;
            log.warn("app.id 未配置，分布式锁前缀回落为固定值 '{}'；多实例部署请显式配置 app.id 以保证锁 key 一致", DEFAULT_LOCK_PREFIX);
        } else {
            prefix = this.appId;
        }
        return new RedissonLockFactory(this.config, prefix);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Aspect.class)
    public DistributeLockedAspect distributeLockedAspect() {
        return new DistributeLockedAspect();
    }

}

