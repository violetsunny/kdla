/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.cache.config;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import top.kdla.framework.supplement.cache.cache.KdlaCafCache;
import top.kdla.framework.supplement.cache.cache.MachineIdHelp;
import top.kdla.framework.supplement.cache.cache.SpringRedisHelp;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kanglele
 * @version $Id: CacheAutoConfig, v 0.1 2023/8/17 19:37 kanglele Exp $
 */
@Configuration
@EnableCaching
@ConditionalOnClass({RedisTemplate.class})
public class CacheAutoConfigure {

    @Value("${app.id:}")
    private String appId;

    @Bean
    public SpringRedisHelp springRedisHelp(@Autowired RedisTemplate redisTemplate) {
        return new SpringRedisHelp(redisTemplate);
    }

    @Bean
    public MachineIdHelp machineIdHelp(SpringRedisHelp springRedisHelp) {
        return new MachineIdHelp(springRedisHelp, appId);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnowflakeGenerator snowflakeGenerator(MachineIdHelp machineIdHelp) {
        return machineIdHelp.getGenerator();
    }


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass({KdlaCafCache.class})
    public CacheManager caffeineCacheManager(@Autowired List<KdlaCafCache<Object, Object>> caffeines) {
        //加入CacheManager为了和@Cacheable一起使用
        List<Cache> caches = new ArrayList<>();
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        for (KdlaCafCache<Object, Object> caffeine : caffeines) {
            caches.add(new CaffeineCache(caffeine.getCacheName(), caffeine.getCafCache()));
        }
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    @PreDestroy
    public void destroy(MachineIdHelp machineIdHelp) {
        machineIdHelp.destroyMachineId();
    }
}
