/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.lock.config;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;
import top.kdla.framework.lock.RedisClusterType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kll
 * @version $Id: RedissonConfig, v 0.1 2021/7/13 9:48 Exp $
 */
@ConfigurationProperties(
        prefix = "kdla.redisson"
)
@Data
public class RedissonConfig {
    private static Logger logger = LoggerFactory.getLogger(RedissonConfig.class);
    /**
     * 必填
     */
    private String address;
    private String password = null;
    private String clientName = null;
    private RedisClusterType type;
    private ReadMode readMode;
    private int scanInterval;
    private String codec;
    /**
     * 选填
     */
    private int connectionMinimumIdleSize = 10;
    private int idleConnectionTimeout = 10000;
    private int pingTimeout = 1000;
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
    private int thread = 4;


    public RedissonConfig() {
        this.readMode = ReadMode.MASTER;
        this.scanInterval = 1000;
        this.type = RedisClusterType.REPLICATE;
        this.codec = "org.redisson.codec.JsonJacksonCodec";
    }

    public List<RedissonClient> redissons() throws Exception {
        List<RedissonClient> clients = new ArrayList();
        if (RedisClusterType.SINGLE.equals(this.type)) {
            clients.add(Redisson.create(this.configSingleNode(this.address, this.password)));
        } else {
            Config config;
            Codec codec;
            if (RedisClusterType.MASTERSLAVE.equals(this.type)) {
                config = new Config();
                ((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)((MasterSlaveServersConfig)config.useMasterSlaveServers().setMasterAddress(this.address).addSlaveAddress(new String[]{this.address}).setMasterConnectionMinimumIdleSize(this.connectionMinimumIdleSize)).setMasterConnectionPoolSize(this.connectionPoolSize)).setSlaveConnectionMinimumIdleSize(this.connectionMinimumIdleSize)).setSlaveConnectionPoolSize(this.connectionPoolSize)).setReadMode(this.readMode)).setDatabase(this.database).setSubscriptionsPerConnection(this.subscriptionsPerConnection)).setClientName(this.clientName)).setFailedSlaveCheckInterval(this.failedAttempts)).setRetryAttempts(this.retryAttempts)).setRetryInterval(this.retryInterval)).setFailedSlaveCheckInterval(this.reconnectionTimeout)).setTimeout(this.timeout)).setConnectTimeout(this.connectTimeout)).setIdleConnectionTimeout(this.idleConnectionTimeout)).setPingConnectionInterval(this.pingTimeout)).setPassword(this.password);
                codec = (Codec) ClassUtils.forName(this.getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
                config.setCodec(codec);
                config.setThreads(this.thread);
                config.setEventLoopGroup(new NioEventLoopGroup());
                config.setTransportMode(TransportMode.NIO);
                clients.add(Redisson.create(config));
            } else if (RedisClusterType.CLUSTER.equals(this.type)) {
                config = new Config();
                ((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)((ClusterServersConfig)config.useClusterServers().addNodeAddress(new String[]{this.address}).setScanInterval(this.scanInterval).setSubscriptionsPerConnection(this.subscriptionsPerConnection)).setClientName(this.clientName)).setFailedSlaveCheckInterval(this.failedAttempts)).setRetryAttempts(this.retryAttempts)).setRetryInterval(this.retryInterval)).setFailedSlaveCheckInterval(this.reconnectionTimeout)).setTimeout(this.timeout)).setConnectTimeout(this.connectTimeout)).setIdleConnectionTimeout(this.idleConnectionTimeout)).setPingConnectionInterval(this.pingTimeout)).setPassword(this.password);
                codec = (Codec)ClassUtils.forName(this.getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
                config.setCodec(codec);
                config.setThreads(this.thread);
                config.setEventLoopGroup(new NioEventLoopGroup());
                config.setTransportMode(TransportMode.NIO);
                clients.add(Redisson.create(config));
            } else if (RedisClusterType.REPLICATE.equals(this.type)) {
                String[] nodes = this.address.split(",");
                int index;
                String[] passwords;
                if (this.password != null) {
                    passwords = this.password.split(",");
                    index = 0;
                    String[] var5 = nodes;
                    int var6 = nodes.length;

                    for(int var7 = 0; var7 < var6; ++var7) {
                        String node = var5[var7];
                        clients.add(Redisson.create(this.configSingleNode(node, passwords[index])));
                        ++index;
                    }
                } else {
                    passwords = nodes;
                    index = nodes.length;

                    for(int var11 = 0; var11 < index; ++var11) {
                        String node = passwords[var11];
                        clients.add(Redisson.create(this.configSingleNode(node, (String)null)));
                    }
                }
            } else if (RedisClusterType.SENTINEL.equals(this.type)) {
            }
        }

        return clients;
    }

    private Config configSingleNode(String address, String password) throws Exception {
        Config config = new Config();
        ((SingleServerConfig)((SingleServerConfig)((SingleServerConfig)((SingleServerConfig)((SingleServerConfig)((SingleServerConfig)((SingleServerConfig)((SingleServerConfig)config.useSingleServer().setAddress(address).setConnectionMinimumIdleSize(this.connectionMinimumIdleSize).setConnectionPoolSize(this.connectionPoolSize).setDatabase(this.database).setDnsMonitoringInterval((long)this.dnsMonitoringInterval).setSubscriptionConnectionMinimumIdleSize(this.subscriptionConnectionMinimumIdleSize).setSubscriptionConnectionPoolSize(this.subscriptionConnectionPoolSize).setSubscriptionsPerConnection(this.subscriptionsPerConnection)).setClientName(this.clientName)).setRetryAttempts(this.retryAttempts)).setRetryInterval(this.retryInterval)).setTimeout(this.timeout)).setConnectTimeout(this.connectTimeout)).setIdleConnectionTimeout(this.idleConnectionTimeout)).setPingConnectionInterval(this.pingTimeout)).setPassword(password);
        Codec codec = (Codec)ClassUtils.forName(this.getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codec);
        config.setThreads(this.thread);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        logger.info("inti the redisson client with config: {}", config.toJSON());
        return config;
    }
}
