/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import top.kdla.framework.common.help.SelfSnowflakeGeneratorHelp;
import top.kdla.framework.supplement.cache.lock.KdlaBizDisLockService;
import top.kdla.framework.supplement.cache.lock.KDistributeLockFactory;
import top.kdla.framework.supplement.cache.lock.KRedissonLockFactory;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;
import top.kdla.framework.supplement.cache.lock.properties.KRedissonConfigProperties;

/**
 * @author kll
 * @version $Id: RedissonAutoConfigure, v 0.1 2021/7/13 9:55 Exp $
 */
@Configuration
@ConditionalOnClass({KRedissonRedDisLock.class})
@EnableConfigurationProperties({KRedissonConfigProperties.class})
@Order
public class KRedissonAutoConfigure {
    @Autowired
    private KRedissonConfigProperties config;
    @Value("${app.id:}")
    private String appId;

    @Bean("disLockService")
    public KdlaBizDisLockService disLockService(KRedissonRedDisLock redissonRedDisLock){
        return new KdlaBizDisLockService(redissonRedDisLock);
    }

    @Bean("redissonRedDisLock")
    @ConditionalOnMissingBean
    public KRedissonRedDisLock redissonRedDisLock(KDistributeLockFactory distributeLockFactory) {
        return new KRedissonRedDisLock(distributeLockFactory);
    }

    @Bean("distributeLockFactory")
    public KDistributeLockFactory distributeLockFactory() {
        String prefix = this.appId == null || "".equals(this.appId) ? SelfSnowflakeGeneratorHelp.generate() : this.appId;
        return new KRedissonLockFactory(this.config, prefix);
    }

}

