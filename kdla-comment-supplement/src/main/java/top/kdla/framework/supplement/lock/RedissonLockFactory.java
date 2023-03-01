/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.lock;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.springframework.util.ClassUtils;
import top.kdla.framework.supplement.lock.properties.RedissonConfigProperties;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author kanglele
 * @version $Id: RedissonLockFactory, v 0.1 2023/2/28 14:36 kanglele Exp $
 */
@Slf4j
public class RedissonLockFactory implements DistributeLockFactory {

    private final static String lockPath = ":lock:";

    private RedissonConfigProperties redissonConfig;

    private List<RedissonClient> redissionClientList;

    private String lockFullPath;

    public RedissonLockFactory(RedissonConfigProperties redissonConfig, String appId) {
        this.redissonConfig = redissonConfig;
        this.redissionClientList = redissons();
        this.lockFullPath = appId + lockPath;
    }

    private List<RedissonClient> redissons() {
        List<RedissonClient> clients = new ArrayList<>();

        if (RedisClusterType.SINGLE.equals(redissonConfig.getType())) {
            clients.add(Redisson.create(configSingleNode(redissonConfig.getAddress(), redissonConfig.getPassword())));
        } else if (RedisClusterType.REPLICATE.equals(redissonConfig.getType())) {
            String[] nodes = redissonConfig.getAddress().split(",");
            if (redissonConfig.getPassword() != null) {
                String[] passwords = redissonConfig.getPassword().split(",");
                int index = 0;
                for (String node : nodes) {
                    clients.add(Redisson.create(configSingleNode(node, passwords[index])));
                    index++;
                }
            } else {
                for (String node : nodes) {
                    clients.add(Redisson.create(configSingleNode(node, null)));
                }
            }
        }
        return clients;
    }

    private Config configSingleNode(String address, String password) {
        Config config = new Config();
        config.useSingleServer().setAddress(address)
                .setConnectionMinimumIdleSize(redissonConfig.getConnectionMinimumIdleSize())
                .setConnectionPoolSize(redissonConfig.getConnectionPoolSize())
                .setDatabase(redissonConfig.getDatabase())
//                .setDnsMonitoring(dnsMonitoring)
                .setDnsMonitoringInterval(redissonConfig.getDnsMonitoringInterval())
                .setSubscriptionConnectionMinimumIdleSize(redissonConfig.getSubscriptionConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(redissonConfig.getConnectionPoolSize())
                .setSubscriptionsPerConnection(redissonConfig.getSubscriptionsPerConnection())
                .setClientName(redissonConfig.getClientName())
//                .setFailedAttempts(failedAttempts)
                .setRetryAttempts(redissonConfig.getRetryAttempts())
                .setRetryInterval(redissonConfig.getRetryInterval())
//                .setReconnectionTimeout(reconnectionTimeout)
                .setTimeout(redissonConfig.getTimeout())
                .setConnectTimeout(redissonConfig.getConnectTimeout())
                .setIdleConnectionTimeout(redissonConfig.getIdleConnectionTimeout())
                //add
                .setPingConnectionInterval(redissonConfig.getPingConnectionInterval())
                .setPassword(password);
        //add
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(redissonConfig.getThread());
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        try {
            log.info("inti the redisson client with config: {}", config.toJSON());
        } catch (IOException ex) {
            log.error("parse json error:", ex);
        }
        return config;
    }

    private Codec getCodecInstance() {
        try {
            return (Codec) ClassUtils.forName(redissonConfig.getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
        } catch (ClassNotFoundException ex) {
            log.error("codec class not found : {}", redissonConfig.getCodec());
            throw new RuntimeException(ex);
        } catch (IllegalAccessException | InstantiationException ex) {
            log.error("get codec error :", ex);
            throw new RuntimeException(ex);
        }
    }

    @PreDestroy
    public void destroy() {
        redissionClientList.stream().forEach(client -> {
            try {
                client.shutdown();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        });
    }

    @Override
    public RLock getLock(String lockKey) {
        String realLockKey = lockFullPath + lockKey;
        List<RLock> rlocks = redissionClientList.stream()
                .map(client -> client.getLock(realLockKey))
                .collect(Collectors.toList());

        return new RedissonRedLock(rlocks.toArray(new RLock[rlocks.size()]));
    }

}
