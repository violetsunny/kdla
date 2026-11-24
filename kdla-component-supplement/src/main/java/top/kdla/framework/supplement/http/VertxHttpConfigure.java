/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kanglele
 * @version $Id: VertxHttpConfig, v 0.1 2023/5/18 10:22 kanglele Exp $
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(name = "kdla.config.vertx-http.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class VertxHttpConfigure {

    @Value("${http.connectionPoolSize:1000}")
    private int connectionPoolSize;

    @Value("${http.connectTimeout:3000}")
    private int connectTimeout;

    @Value("${http.socketTimeout:3000}")
    private int socketTimeout;

    @Value("${http.idleTimeout:10}")
    private int idleTimeout;

    @Value("${http.maxWaitQueueSize:500}")
    private int maxWaitQueueSize;

    @Value("${http.sslHandshakeTimeout:10000}")
    private long sslHandshakeTimeout;

    //HTTPS是否信任所有证书；默认false(校验证书链)，硬编码true存在中间人攻击(MITM)风险
    @Value("${kdla.http.ssl.trust-all:false}")
    private boolean sslTrustAll;

    //单次HTTP请求整体超时(ms)，防止服务端无响应时调用方线程永久阻塞
    @Value("${kdla.http.request-timeout:30000}")
    private long requestTimeout;

    @Bean
    @ConditionalOnMissingBean(name = "vertxHttpClient")
    //两个同类型WebClient须显式Qualifier：Spring 6.1移除LVT参数名发现器，未开-parameters时按参数名消歧失效致启动NoUniqueBeanDefinitionException
    public VertxHttpClient vertxHttpClient(@Qualifier("webClient") WebClient webClient, @Qualifier("webClientHttps") WebClient webClientHttps) {
        if (log.isInfoEnabled()) {
            log.info("VertxHttpClient init Bean");
        }
        VertxHttpClient client = new VertxHttpClient(webClient,webClientHttps);
        client.setRequestTimeoutMs(requestTimeout);
        return client;
    }

    @Bean
    @ConditionalOnMissingBean(name = "webClient")
    public WebClient webClient(Vertx vertx) {
        return WebClient.create(vertx, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_1_1)
                .setKeepAlive(true)
                .setMaxPoolSize(connectionPoolSize)
                .setWebSocketClosingTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setIdleTimeout(idleTimeout)
                .setMaxWaitQueueSize(maxWaitQueueSize)
                //不跟随重定向：302指向内网可绕过首跳URL校验(SSRF)，3xx原样返回由调用方校验Location后自行发起
                .setMaxRedirects(0));
    }

    @Bean
    @ConditionalOnMissingBean(name = "webClientHttps")
    public WebClient webClientHttps(Vertx vertx) {
        if (sslTrustAll) {
            log.warn("kdla.http.ssl.trust-all=true 已开启，HTTPS将信任所有证书，存在中间人攻击(MITM)风险，生产环境请务必关闭");
        }
        return WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(sslTrustAll)
                .setProtocolVersion(HttpVersion.HTTP_1_1)
                .setKeepAlive(true)
                .setMaxPoolSize(connectionPoolSize)
                .setWebSocketClosingTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setIdleTimeout(idleTimeout)
                .setMaxWaitQueueSize(maxWaitQueueSize)
                .setSslHandshakeTimeout(sslHandshakeTimeout)
                //不跟随重定向：302指向内网可绕过首跳URL校验(SSRF)，3xx原样返回由调用方校验Location后自行发起
                .setMaxRedirects(0));
    }

    @Bean
    @ConditionalOnMissingBean(Vertx.class)
    public Vertx vertx() {
        return Vertx.vertx();
    }

}
