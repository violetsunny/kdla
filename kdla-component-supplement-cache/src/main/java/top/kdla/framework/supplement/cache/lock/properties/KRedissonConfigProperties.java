/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock.properties;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import lombok.ToString;
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
import top.kdla.framework.supplement.cache.lock.RedisClusterType;

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
    @ToString.Exclude
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

    //SENTINEL模式必填：Redis主节点名（kdla.redisson.master-name）
    private String masterName;

    /**
     * 解析逗号分隔的节点地址：null/空白/仅逗号一律fail fast，多节点trim并过滤空串。
     */
    public static List<String> splitNodes(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("kdla.redisson.address 不能为空，请配置至少一个Redis节点地址");
        }
        List<String> nodes = new ArrayList<>();
        for (String node : address.split(",")) {
            String trimmed = node.trim();
            if (!trimmed.isEmpty()) {
                nodes.add(trimmed);
            }
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("kdla.redisson.address 未解析出有效节点地址：" + address);
        }
        return nodes;
    }

    public List<RedissonClient> redissons(){
        List<RedissonClient> clients = new ArrayList<>();

        if(RedisClusterType.SINGLE.equals(type)){
            //地址空白时fail fast，避免把空串传给Redisson产生晦涩报错
            splitNodes(address);
            clients.add(Redisson.create(configSingleNode(address.trim(), password)));
        }else if(RedisClusterType.MASTER_SLAVE.equals(type)){
            //TODO 3.2 master slave not support dns configure
            splitNodes(address);
            clients.add(Redisson.create(configMasterSlave()));
        }else if(RedisClusterType.CLUSTER.equals(type)){
            clients.add(Redisson.create(configCluster()));
        }else if(RedisClusterType.REPLICATE.equals(type)){
            List<String> nodes = splitNodes(address);
            if(password!=null){
                String[] passwords = password.split(",");
                if (passwords.length != nodes.size()) {
                    throw new IllegalArgumentException(
                            "REPLICATE模式下密码数量(" + passwords.length + ")与节点数量(" + nodes.size() + ")不匹配");
                }
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
            splitNodes(address);
            if(masterName==null || masterName.trim().isEmpty()){
                throw new IllegalArgumentException("SENTINEL模式必须配置 kdla.redisson.master-name（Redis主节点名）");
            }
            clients.add(Redisson.create(configSentinel()));
        }else{
            //type为null/未知时fail fast，杜绝静默返回空client列表导致零节点RedLock锁失效
            throw new IllegalArgumentException("不支持的 kdla.redisson.type: " + type);
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
                .setPassword(password)
                .setPingConnectionInterval(pingConnectionInterval);
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(thread);
        config.setNettyThreads(nettyThreads);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        // toYAML输出含password明文，改为仅记录非敏感项
        if (log.isInfoEnabled()) {
            log.info("init the redisson client, address:{}, codec:{}, threads:{}", address, codec, thread);
        }
        return config;
    }

    private Config configMasterSlave(){
        //address首节点为master，其余为slave
        List<String> nodes = splitNodes(address);
        String master = nodes.get(0);
        String[] slaves = nodes.subList(1, nodes.size()).toArray(new String[0]);
        Config config = new Config();
        config.useMasterSlaveServers().setMasterAddress(master)
                .addSlaveAddress(slaves)
                .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setMasterConnectionPoolSize(connectionPoolSize)
                .setSlaveConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSlaveConnectionPoolSize(connectionPoolSize)
                .setReadMode(readMode)
                .setDatabase(database)
                .setSubscriptionsPerConnection(subscriptionsPerConnection)
                .setClientName(clientName)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setFailedSlaveCheckInterval(reconnectionTimeout)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setPassword(password)
                .setPingConnectionInterval(pingConnectionInterval);
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(thread);
        config.setNettyThreads(nettyThreads);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        if (log.isInfoEnabled()) {
            log.info("init the redisson master-slave client, master:{}, slaveCount:{}, codec:{}", master, slaves.length, codec);
        }
        return config;
    }

    private Config configCluster(){
        Config config = new Config();
        //旧实现把整个逗号分隔串当单个节点地址传入，多节点配置不生效；改为逐节点添加
        List<String> clusterNodes = splitNodes(address);
        config.useClusterServers()
                .addNodeAddress(clusterNodes.toArray(new String[0]))
                .setScanInterval(scanInterval)
                .setSubscriptionsPerConnection(subscriptionsPerConnection)
                .setClientName(clientName)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setFailedSlaveCheckInterval(reconnectionTimeout)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
//                .setPingTimeout(pingTimeout)
                .setPassword(password)
                .setPingConnectionInterval(pingConnectionInterval);
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(thread);
        config.setNettyThreads(nettyThreads);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        if (log.isInfoEnabled()) {
            log.info("init the redisson cluster client, nodeCount:{}, codec:{}", clusterNodes.size(), codec);
        }
        return config;
    }

    private Config configSentinel(){
        List<String> sentinelNodes = splitNodes(address);
        Config config = new Config();
        config.useSentinelServers()
                .setMasterName(masterName.trim())
                .addSentinelAddress(sentinelNodes.toArray(new String[0]))
                .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setMasterConnectionPoolSize(connectionPoolSize)
                .setSlaveConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setSlaveConnectionPoolSize(connectionPoolSize)
                .setReadMode(readMode)
                .setDatabase(database)
                .setSubscriptionsPerConnection(subscriptionsPerConnection)
                .setClientName(clientName)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setFailedSlaveCheckInterval(reconnectionTimeout)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setPassword(password)
                .setPingConnectionInterval(pingConnectionInterval);
        Codec codec = getCodecInstance();
        config.setCodec(codec);
        config.setThreads(thread);
        config.setNettyThreads(nettyThreads);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setTransportMode(TransportMode.NIO);
        if (log.isInfoEnabled()) {
            log.info("init the redisson sentinel client, masterName:{}, sentinelCount:{}, codec:{}", masterName, sentinelNodes.size(), codec);
        }
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
