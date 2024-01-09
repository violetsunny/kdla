/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.mqtt;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.Random;

/**
 * @author kanglele
 * @version $Id: VertxHttpConfig, v 0.1 2023/5/18 10:22 kanglele Exp $
 */
@Configuration
@Slf4j
public class VertxMqttConfigure {

    @Value("${kdla.mqtt.clientId:kdla-clientId}")
    private String clientId;

    @Value("${kdla.mqtt.username:-1}")
    private String username;

    @Value("${kdla.mqtt.password:-1}")
    private String password;

    @Value("${kdla.mqtt.host:localhost}")
    private String host;

    @Value("${kdla.mqtt.port:1883}")
    private int port;

    @Value("${kdla.mqtt.acktimeout:60000}")
    private int acktimeout;

    @Bean
    public VertxMqttClient vertxMqttClient(MqttClient mqttClient) {
        return new VertxMqttClient(mqttClient);
    }

    @Bean
    @ConditionalOnMissingBean(MqttClient.class)
    public MqttClient mqttClient(Vertx vertx) {
        clientId = clientId + new Random().nextInt(10000);
        MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions()
                .setClientId(clientId)
                .setUsername(username)
                .setPassword(password)
                .setAckTimeout(acktimeout)
                .setAutoKeepAlive(true));
        mqttClient.connect(port, host, res -> {
            if (!res.succeeded()) {
                if (log.isWarnEnabled()) {
                    log.warn("connect mqtt [{}] error", clientId, res.cause());
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info("connect mqtt [{}] success", clientId);
                }
            }
        });
        return mqttClient;
    }

    @Bean
    @ConditionalOnMissingBean(Vertx.class)
    public Vertx vertx() {
        return Vertx.vertx();
    }

//    @PreDestroy
//    public void close() {
//        vertx().close();
//    }


}
