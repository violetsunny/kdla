/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.lock.properties;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.TransportMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;
import top.kdla.framework.supplement.lock.RedisClusterType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kll
 * @version $Id: RedissonConfig, v 0.1 2021/7/13 9:48 Exp $
 */
@ConditionalOnMissingBean(RedissonClient.class)
@ConfigurationProperties(
        prefix = "kdla.redisson"
)
@Data
@Slf4j
public class RedissonConfigProperties {
    /**
     * 必填
     */
    //format: 127.0.0.1:7000,127.0.0.1:7001;
    private String address;
    //multi nodes, password split by comma(,)
    private String password = null;
    private String clientName = null;
    private RedisClusterType type;
    private ReadMode readMode;
    private int scanInterval;
    private String codec;
    /**
     * 选填
     */
    //single node properties
    private int connectionMinimumIdleSize = 10;
    private int idleConnectionTimeout = 10000;
    private int connectTimeout = 10000;
    private int timeout = 3000;
    private int retryAttempts = 3;
    private int retryInterval = 1500;
    private int reconnectionTimeout = 3000;
    private int failedAttempts = 3;
    private int subscriptionsPerConnection = 5;
    private int subscriptionConnectionMinimumIdleSize = 1;
    private int subscriptionConnectionPoolSize = 50;
    private int connectionPoolSize = 64;
    private int database = 0;
    private boolean dnsMonitoring = false;
    private int dnsMonitoringInterval = 5000;
    //unlock失败重试次数
    private int unlockRetry = 3;
    private int thread; //当前处理核数量 * 2
    private int pingConnectionInterval = 5000;


    public RedissonConfigProperties() {
        this.readMode = ReadMode.MASTER;
        this.scanInterval = 1000;
        this.type = RedisClusterType.REPLICATE;
        this.codec = "org.redisson.codec.JsonJacksonCodec";
    }

    public List<RedissonClient> redissons() {
        List<RedissonClient> clients = new ArrayList<>();

        if (RedisClusterType.SINGLE.equals(this.type)) {
            clients.add(Redisson.create(configSingleNode(this.address, this.password)));
        } else if (RedisClusterType.MASTERSLAVE.equals(this.type)) {
            //TODO 3.2 master slave not support dns configure
            clients.add(Redisson.create(configMasterSlave()));
        } else if (RedisClusterType.CLUSTER.equals(this.type)) {
            clients.add(Redisson.create(configCluster()));
        } else if (RedisClusterType.REPLICATE.equals(this.type)) {
            String[] nodes = this.address.split(",");
            if (this.password != null) {
                String[] passwords = this.password.split(",");
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
        } else if (RedisClusterType.SENTINEL.equals(this.type)) {
            //TODO
        }

        return clients;
    }

    private Config configSingleNode(String address, String password) {
        Config config = new Config();
        config.useSingleServer().setAddress(address)
                .setConnectionMinimumIdleSize(this.connectionMinimumIdleSize)
                .setConnectionPoolSize(this.connectionPoolSize)
                .setDatabase(this.database)
//                .setDnsMonitoring(dnsMonitoring)
                .setDnsMonitoringInterval(this.dnsMonitoringInterval)
                .setSubscriptionConnectionMinimumIdleSize(this.subscriptionConnectionMinimumIdleSize)
                .setSubscriptionConnectionPoolSize(this.subscriptionConnectionPoolSize)
                .setSubscriptionsPerConnection(this.subscriptionsPerConnection)
                .setClientName(this.clientName)
//                .setFailedAttempts(failedAttempts)
                .setRetryAttempts(this.retryAttempts)
                .setRetryInterval(this.retryInterval)
//                .setReconnectionTimeout(reconnectionTimeout)
                .setTimeout(this.timeout)
                .setConnectTimeout(this.connectTimeout)
                .setIdleConnectionTimeout(this.idleConnectionTimeout)
                .setPassword(password)
                .setPingConnectionInterval(this.pingConnectionInterval);
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(this.thread);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        try {
            log.info("inti the redisson client with config: {}", config.toJSON());
        } catch (IOException ex) {
            log.error("parse json error:", ex);
        }
        return config;
    }

    private Config configMasterSlave() {
        Config config = new Config();
        config.useMasterSlaveServers().setMasterAddress(this.address)
                .addSlaveAddress(this.address)
                .setMasterConnectionMinimumIdleSize(this.connectionMinimumIdleSize)
                .setMasterConnectionPoolSize(this.connectionPoolSize)
                .setSlaveConnectionMinimumIdleSize(this.connectionMinimumIdleSize)
                .setSlaveConnectionPoolSize(this.connectionPoolSize)
                .setReadMode(this.readMode)
                .setDatabase(this.database)
                .setSubscriptionsPerConnection(this.subscriptionsPerConnection)
                .setClientName(this.clientName)
                .setFailedSlaveCheckInterval(this.failedAttempts)
                .setRetryAttempts(this.retryAttempts)
                .setRetryInterval(this.retryInterval)
                .setFailedSlaveCheckInterval(this.reconnectionTimeout)
                .setTimeout(this.timeout)
                .setConnectTimeout(this.connectTimeout)
                .setIdleConnectionTimeout(this.idleConnectionTimeout)
                .setPassword(this.password)
                .setPingConnectionInterval(this.pingConnectionInterval);
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(this.thread);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        return config;
    }

    private Config configCluster() {
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress(this.address)
                .setScanInterval(this.scanInterval)
                .setSubscriptionsPerConnection(this.subscriptionsPerConnection)
                .setClientName(this.clientName)
                .setFailedSlaveCheckInterval(this.failedAttempts)
                .setRetryAttempts(this.retryAttempts)
                .setRetryInterval(this.retryInterval)
                .setFailedSlaveCheckInterval(this.reconnectionTimeout)
                .setTimeout(this.timeout)
                .setConnectTimeout(this.connectTimeout)
                .setIdleConnectionTimeout(this.idleConnectionTimeout)
                .setPassword(this.password)
                .setPingConnectionInterval(this.pingConnectionInterval);
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(this.thread);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        return config;
    }

    private Codec getCodecInstance() {
        try {
            return (Codec) ClassUtils.forName(getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
        } catch (ClassNotFoundException ex) {
            log.error("codec class not found : {}", getCodec());
            throw new RuntimeException(ex);
        } catch (IllegalAccessException | InstantiationException ex) {
            log.error("get codec error :", ex);
            throw new RuntimeException(ex);
        }
    }
}
