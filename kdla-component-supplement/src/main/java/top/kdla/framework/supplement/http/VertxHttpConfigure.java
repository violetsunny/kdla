/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

/**
 * @author kanglele
 * @version $Id: VertxHttpConfig, v 0.1 2023/5/18 10:22 kanglele Exp $
 */
@Configuration
public class VertxHttpConfigure {

    @Value("${http.connectionPoolSize:1000}")
    private int connectionPoolSize;

    @Value("${http.connectTimeout:1000}")
    private int connectTimeout;

    @Value("${http.socketTimeout:3000}")
    private int socketTimeout;

    @Value("${http.idleTimeout:10}")
    private int idleTimeout;

    @Value("${http.maxWaitQueueSize:500}")
    private int maxWaitQueueSize;

    @Bean
    @ConditionalOnMissingBean(VertxHttpClient.class)
    public VertxHttpClient vertxHttpClient() {
        return new VertxHttpClient(webClient());
    }

    @Bean
    @ConditionalOnMissingBean(WebClient.class)
    public WebClient webClient() {
        return WebClient.create(vertx(), new WebClientOptions()
//                .setSsl(true)
//                .setTrustAll(true)
                .setProtocolVersion(HttpVersion.HTTP_1_1)
                .setKeepAlive(true)
                .setMaxPoolSize(connectionPoolSize)
                .setWebSocketClosingTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setIdleTimeout(idleTimeout)
                .setMaxWaitQueueSize(maxWaitQueueSize));
    }

    @Bean
    public Vertx vertx() {
        return Vertx.vertx();
    }

    @PreDestroy
    public void close() {
        webClient().close();
        vertx().close();
    }


}
