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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kanglele
 * @version $Id: VertxHttpConfig, v 0.1 2023/5/18 10:22 kanglele Exp $
 */
@Configuration
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

    @Bean
    @ConditionalOnMissingBean(name = "vertxHttpClient")
    public VertxHttpClient vertxHttpClient(WebClient webClient,WebClient webClientHttps) {
        if (log.isInfoEnabled()) {
            log.info("VertxHttpClient init Bean");
        }
        return new VertxHttpClient(webClient,webClientHttps);
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
                .setMaxWaitQueueSize(maxWaitQueueSize));
    }

    @Bean
    @ConditionalOnMissingBean(name = "webClientHttps")
    public WebClient webClientHttps(Vertx vertx) {
        return WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setProtocolVersion(HttpVersion.HTTP_1_1)
                .setKeepAlive(true)
                .setMaxPoolSize(connectionPoolSize)
                .setWebSocketClosingTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setIdleTimeout(idleTimeout)
                .setMaxWaitQueueSize(maxWaitQueueSize)
                .setSslHandshakeTimeout(sslHandshakeTimeout));
    }

    @Bean
    @ConditionalOnMissingBean(Vertx.class)
    public Vertx vertx() {
        return Vertx.vertx();
    }

}
