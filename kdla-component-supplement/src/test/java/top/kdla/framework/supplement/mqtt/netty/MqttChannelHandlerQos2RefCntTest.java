package top.kdla.framework.supplement.mqtt.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.jupiter.api.Test;
import top.kdla.framework.supplement.mqtt.MqttHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单#14 回归测试（移植自1.x）：MqttChannelHandler QoS2流程ByteBuf引用计数修复。
 *
 * <p>旧实现三处问题：QoS0/1末尾额外release与autoRelease叠加双重释放；
 * QoS2存储前未retain致payload被autoRelease释放；PUBREL读已释放payload且未配对释放。
 *
 * <p>无修复则失败：QoS0用例writeInbound抛IllegalReferenceCountException；
 * QoS2用例存储后refCnt==0（断言1失败），PUBREL阶段读payload抛异常。
 */
class MqttChannelHandlerQos2RefCntTest {

    private static final String TOPIC = "test/topic";

    @Test
    void qos2PublishRetainedUntilPubrel_thenReleasedExactlyOnce() {
        List<String> received = new ArrayList<>();
        MqttClientImpl client = new MqttClientImpl((MqttHandler) null);
        client.getSubscriptions().put(TOPIC, new MqttSubscription(TOPIC, newRecordingHandler(received), false));
        EmbeddedChannel channel = new EmbeddedChannel(new MqttChannelHandler(client, null));
        // 丢弃channelActive时写出的CONNECT报文
        channel.readOutbound();

        ByteBuf payload = Unpooled.wrappedBuffer("qos2-payload".getBytes(StandardCharsets.UTF_8));
        MqttPublishMessage publish = new MqttPublishMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.EXACTLY_ONCE, false, 0),
                new MqttPublishVariableHeader(TOPIC, 1), payload);

        channel.writeInbound(publish);

        // 存储等待PUBREL期间payload被retain（refCnt=1），未被autoRelease释放
        assertEquals(1, payload.refCnt());
        assertEquals(1, client.getQos2PendingIncomingPublishes().size());
        assertTrue(received.isEmpty());

        MqttMessage pubrel = new MqttMessage(
                new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(1));
        channel.writeInbound(pubrel);

        // PUBREL后handler收到完整payload内容（旧实现此处use-after-free）
        assertEquals(1, received.size());
        assertEquals("qos2-payload", received.get(0));
        // retain/release一一配对，最终refCnt=0无泄漏；pending队列清空
        assertEquals(0, payload.refCnt());
        assertEquals(0, client.getQos2PendingIncomingPublishes().size());

        channel.finish();
    }

    @Test
    void qos0Publish_releasedExactlyOnceByAutoRelease() {
        List<String> received = new ArrayList<>();
        MqttClientImpl client = new MqttClientImpl((MqttHandler) null);
        client.getSubscriptions().put(TOPIC, new MqttSubscription(TOPIC, newRecordingHandler(received), false));
        EmbeddedChannel channel = new EmbeddedChannel(new MqttChannelHandler(client, null));
        channel.readOutbound();

        ByteBuf payload = Unpooled.wrappedBuffer("qos0-payload".getBytes(StandardCharsets.UTF_8));
        MqttPublishMessage publish = new MqttPublishMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                new MqttPublishVariableHeader(TOPIC, -1), payload);

        // 旧实现末尾多余release与autoRelease叠加，双重释放抛IllegalReferenceCountException
        channel.writeInbound(publish);

        assertEquals(1, received.size());
        assertEquals("qos0-payload", received.get(0));
        assertEquals(0, payload.refCnt());

        channel.finish();
    }

    private MqttHandler newRecordingHandler(List<String> received) {
        return new MqttHandler() {
            @Override
            public Pattern topicPattern() {
                return Pattern.compile(TOPIC);
            }

            @Override
            public void onMessage(String topic, ByteBuf payload) {
                byte[] bytes = new byte[payload.readableBytes()];
                payload.getBytes(payload.readerIndex(), bytes);
                received.add(new String(bytes, StandardCharsets.UTF_8));
            }
        };
    }
}