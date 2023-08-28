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
import top.kdla.framework.supplement.cache.lock.DistributeLockFactory;
import top.kdla.framework.supplement.cache.lock.RedissonLockFactory;
import top.kdla.framework.supplement.cache.lock.RedissonRedDisLock;
import top.kdla.framework.supplement.cache.lock.properties.RedissonConfigProperties;

import java.util.UUID;

/**
 * @author kll
 * @version $Id: RedissonAutoConfigure, v 0.1 2021/7/13 9:55 Exp $
 */
@Configuration
@ConditionalOnClass({RedissonRedDisLock.class})
@EnableConfigurationProperties({RedissonConfigProperties.class})
@Order
public class RedissonAutoConfigure {
    @Autowired
    private RedissonConfigProperties config;
    @Value("${app.id:}")
    private String appId;

    @Bean("disLockService")
    public KdlaBizDisLockService disLockService(RedissonRedDisLock redissonRedDisLock){
        return new KdlaBizDisLockService(redissonRedDisLock);
    }

    @Bean("redissonRedDisLock")
    @ConditionalOnMissingBean
    public RedissonRedDisLock redissonRedDisLock(DistributeLockFactory distributeLockFactory) {
        return new RedissonRedDisLock(distributeLockFactory);
    }

    @Bean("distributeLockFactory")
    public DistributeLockFactory distributeLockFactory() {
        String prefix = this.appId == null || "".equals(this.appId) ? SelfSnowflakeGeneratorHelp.generate() : this.appId;
        return new RedissonLockFactory(this.config, prefix);
    }

}

