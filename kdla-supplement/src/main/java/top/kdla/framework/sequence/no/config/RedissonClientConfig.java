package top.kdla.framework.sequence.no.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 基于rdfa配置的RedissonClient配置类。
 * 当管理容器中没有找到RedissonClient bean的时候，自动创建注入。
 * @author kll
 * @date 2022/1/11
 */
//@Slf4j
//@ConditionalOnMissingBean(RedissonClient.class)
//@Configuration
//@Order
//public class RedissonClientConfig {
//
//    @Value("${kdla.redisson.database}")
//    private Integer database;
//    @Value("${kdla.redisson.password}")
//    private String password;
//    @Value("${kdla.redisson.address}")
//    private String address;
//
//
//    @Bean(destroyMethod = "shutdown")
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        config.useSingleServer().setAddress(address).setPassword(password).setDatabase(database);
//        RedissonClient redisson = Redisson.create(config);
//
//        log.info("RedissonClient injeted...");
//        return redisson;
//    }
//}
