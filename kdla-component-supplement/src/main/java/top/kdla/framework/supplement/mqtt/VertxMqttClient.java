/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import lombok.extern.slf4j.Slf4j;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;

/**
 * @author kanglele
 * @version $Id: VertxMqttClient, v 0.1 2023/9/18 16:40 kanglele Exp $
 */
@Slf4j
public class VertxMqttClient {

    private final MqttClient mqttClient;

    public VertxMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void send(String topic, String msg, int qos) {
        mqttClient.publish(topic,
                Buffer.buffer(msg),
                MqttQoS.valueOf(qos),
                false,
                false,
                res -> {
                    if (res.succeeded()) {
                        log.debug("publish mqtt [{}] message success", mqttClient.clientId());
                    } else {
                        log.warn("publish mqtt [{}] message error", mqttClient.clientId(), res.cause());
                        throw new BizException(ErrorCode.FAIL.getCode(), "发送mqtt失败", res.cause());
                    }
                });
    }

}
