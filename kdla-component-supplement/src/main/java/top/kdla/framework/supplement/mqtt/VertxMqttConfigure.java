/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kdla.framework.domain.ApplicationContextHelp;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

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

    @Value("${kdla.mqtt.topics}")
    private List<String> topics;

    @Value("${kdla.mqtt.ack.timeout:60000}")
    private int acktimeout;

    @Value("${kdla.mqtt.ack.retry:1000}")
    private long retry;

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

        connect(mqttClient);

        mqttClient.closeHandler(v -> {
            vertx.setTimer(retry, id -> {
                connect(mqttClient);
            });
        });
        return mqttClient;
    }

    private void connect(MqttClient mqttClient){
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
        if(CollectionUtils.isNotEmpty(topics)){
            IntStream.range(0, topics.size())
                    .forEach(i -> {
                        mqttClient.subscribe(topics.get(i), MqttQoS.AT_MOST_ONCE.value())
                                .onSuccess(id -> {
                                    log.info("topic={} subscribe success", topics.get(i));
                                })
                                .onFailure(throwable -> {
                                    log.error("topic subscribe failed", throwable);
                                });
                    });

            Map<String, MqttHandler> mqttHandlers = ApplicationContextHelp.getBeansOfType(MqttHandler.class);

            mqttClient.publishHandler(message -> {
                // 处理接收到的消息
                String topicName = message.topicName();
                Buffer payload = message.payload();
                System.out.println("Received message from topic: " + topicName + " with payload: " + payload.toString());
                // 可以添加更多的业务逻辑处理

               if(MapUtils.isNotEmpty(mqttHandlers)){
                   MqttHandler mqttHandlerImpl = mqttHandlers.values().stream().filter(mqttHandler -> mqttHandler.topicPattern().matcher(topicName).matches()).findFirst().orElse(null);
                   if(mqttHandlerImpl!=null){
                       byte[] bytes = payload.getBytes();
                       mqttHandlerImpl.onMessage(topicName,Unpooled.wrappedBuffer(bytes));
                   }
               }

            });
        }

    }

    @Bean
    @ConditionalOnMissingBean(Vertx.class)
    public Vertx vertx() {
        return Vertx.vertx();
    }

}
