/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.lock.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import top.kdla.framework.lock.RedissonRedDisLock;

/**
 * @author kll
 * @version $Id: RedissonAutoConfigure, v 0.1 2021/7/13 9:55 Exp $
 */
@Configuration
@ConditionalOnClass({RedissonRedDisLock.class})
@EnableConfigurationProperties({RedissonConfig.class})
@Order
public class RedissonAutoConfigure {
    @Autowired
    private RedissonConfig config;

    public RedissonAutoConfigure() {
    }

    @Bean("redissonRedDisLock")
    @ConditionalOnMissingBean
    RedissonRedDisLock redissonRedDisLock() {
        return new RedissonRedDisLock(this.config);
    }
}

