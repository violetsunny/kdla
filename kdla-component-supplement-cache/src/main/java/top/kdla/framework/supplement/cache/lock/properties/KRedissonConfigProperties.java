/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock.properties;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.config.ConfigSupport;
import org.redisson.config.ReadMode;
import org.redisson.config.TransportMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;
import top.kdla.framework.common.help.KdlaStringHelp;
import top.kdla.framework.supplement.cache.lock.RedisClusterType;

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
public class KRedissonConfigProperties {
    /**
     * 必填
     */
    //format: 127.0.0.1:7000,127.0.0.1:7001;
    private String address;

    //single node properties
    private int connectionMinimumIdleSize = 10;
    private int idleConnectionTimeout=10000;
    private int pingTimeout=1000;
    private int connectTimeout=10000;
    private int timeout=3000;
    private int retryAttempts=3;
    private int retryInterval=1500;
    private int reconnectionTimeout=3000;
    private int failedAttempts=3;
    //multi nodes, password split by comma(,)
    private String password = null;
    private int subscriptionsPerConnection=5;
    private String clientName=null;
    private int subscriptionConnectionMinimumIdleSize = 1;
    private int subscriptionConnectionPoolSize = 50;
    private int connectionPoolSize = 64;
    private int database = 0;
    private boolean dnsMonitoring = false;
    private int dnsMonitoringInterval = 5000;

    //master slave properties
    private ReadMode readMode = ReadMode.MASTER;

    //cluster properties
    private int scanInterval = 1000;
    //unlock失败重试次数
    private int unlockRetry = 3;

    private RedisClusterType type = RedisClusterType.REPLICATE;

    private int thread; //当前处理核数量 * 2

    private String codec="org.redisson.codec.JsonJacksonCodec";

    private int pingConnectionInterval = 5000;


    private int nettyThreads = 32;

    public List<RedissonClient> redissons(){
        List<RedissonClient> clients = new ArrayList<>();

        if(RedisClusterType.SINGLE.equals(type)){
            clients.add(Redisson.create(configSingleNode(address, password)));
        }else if(RedisClusterType.MASTER_SLAVE.equals(type)){
            //TODO 3.2 master slave not support dns configure
            clients.add(Redisson.create(configMasterSlave()));
        }else if(RedisClusterType.CLUSTER.equals(type)){
            clients.add(Redisson.create(configCluster()));
        }else if(RedisClusterType.REPLICATE.equals(type)){
            String[] nodes = address.split(",");
            if(password!=null){
                String[] passwords = password.split(",");
                int index = 0;
                for(String node : nodes){
                    clients.add(Redisson.create(configSingleNode(node, passwords[index])));
                    index++;
                }
            }else{
                for(String node : nodes){
                    clients.add(Redisson.create(configSingleNode(node, null)));
                }
            }
        }else if(RedisClusterType.SENTINEL.equals(type)){
            //TODO
        }

        return clients;
    }

    private Config configSingleNode(String address, String password) {
        Config config = new Config();
        config.useSingleServer().setAddress(address)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setDatabase(database)
//                .setDnsMonitoring(dnsMonitoring)
                .setDnsMonitoringInterval(dnsMonitoringInterval)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setSubscriptionsPerConnection(subscriptionsPerConnection)
                .setClientName(clientName)
//                .setFailedAttempts(failedAttempts)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
//                .setReconnectionTimeout(reconnectionTimeout)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
//                .setPingTimeout(pingTimeout)
//                .setPassword(password)
                .setPingConnectionInterval(pingConnectionInterval);
        if(KdlaStringHelp.isNotEmpty(password)){
            config.useSingleServer().setPassword(password);
        }
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(thread);
        config.setNettyThreads(nettyThreads);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        try {
            ConfigSupport support = new ConfigSupport();
            log.info("init the redisson client with config: {}",  support.toJSON(config));
        }
        catch (IOException ex){
            log.error("parse json error:",ex);
        }
        return config;
    }

    private Config configMasterSlave(){
        Config config = new Config();
        config.useMasterSlaveServers().setMasterAddress(address)
                .addSlaveAddress(address)
                .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setMasterConnectionPoolSize(connectionPoolSize)
                .setSlaveConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSlaveConnectionPoolSize(connectionPoolSize)
                .setReadMode(readMode)
                .setDatabase(database)
                .setSubscriptionsPerConnection(subscriptionsPerConnection)
                .setClientName(clientName)
                .setFailedSlaveCheckInterval(failedAttempts)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setFailedSlaveCheckInterval(reconnectionTimeout)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
//                .setPingTimeout(pingTimeout)
//                .setPassword(password)
                .setPingConnectionInterval(pingConnectionInterval);
        if(KdlaStringHelp.isNotEmpty(password)){
            config.useSingleServer().setPassword(password);
        }
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(thread);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        return config;
    }

    private Config configCluster(){
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress(address)
                .setScanInterval(scanInterval)
                .setSubscriptionsPerConnection(subscriptionsPerConnection)
                .setClientName(clientName)
                .setFailedSlaveCheckInterval(failedAttempts)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setFailedSlaveCheckInterval(reconnectionTimeout)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
//                .setPingTimeout(pingTimeout)
//                .setPassword(password)
                .setPingConnectionInterval(pingConnectionInterval);
        if(KdlaStringHelp.isNotEmpty(password)){
            config.useSingleServer().setPassword(password);
        }
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(thread);
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
