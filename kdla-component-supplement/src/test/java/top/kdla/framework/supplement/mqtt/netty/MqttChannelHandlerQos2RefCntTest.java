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
 * 清单#14 回归测试：MqttChannelHandler QoS2 流程 ByteBuf 引用计数修复。
 *
 * <p>旧实现三处问题：
 * <ol>
 * <li>invokeHandlersForIncomingPublish 末尾额外 release，与 SimpleChannelInboundHandler
 * 的 autoRelease 叠加，QoS0/1 双重释放抛 IllegalReferenceCountException；</li>
 * <li>QoS2 PUBLISH 存储进 pending 队列前未 retain，channelRead0 返回后 autoRelease
 * 即释放 payload；</li>
 * <li>PUBREL 到达时读取已释放的 payload（use-after-free），且处理后未配对释放。</li>
 * </ol>
 *
 * <p>修复后：QoS0/1 由 autoRelease 唯一释放；QoS2 存储前 retain，PUBREL 处理完
 * finally 中 release，retain/release 一一配对。
 *
 * <p>无修复则失败：QoS0 用例 writeInbound 直接抛 IllegalReferenceCountException；
 * QoS2 用例存储后 refCnt==0（断言 1 失败），PUBREL 阶段读 payload 抛异常。
 *
 * <p>说明：使用 EmbeddedChannel + 真实 MqttClientImpl（包私有，同包测试直接构造），
 * 不需要真实 MQTT broker。
 */
class MqttChannelHandlerQos2RefCntTest {

    private static final String TOPIC = "test/topic";

    @Test
    void qos2PublishRetainedUntilPubrel_thenReleasedExactlyOnce() {
        List<String> received = new ArrayList<>();
        MqttClientImpl client = new MqttClientImpl((MqttHandler) null);
        client.getSubscriptions().put(TOPIC, new MqttSubscription(TOPIC, newRecordingHandler(received), false));
        EmbeddedChannel channel = new EmbeddedChannel(new MqttChannelHandler(client, null));
        // 丢弃 channelActive 时写出的 CONNECT 报文
        channel.readOutbound();

        ByteBuf payload = Unpooled.wrappedBuffer("qos2-payload".getBytes(StandardCharsets.UTF_8));
        MqttPublishMessage publish = new MqttPublishMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.EXACTLY_ONCE, false, 0),
                new MqttPublishVariableHeader(TOPIC, 1), payload);

        channel.writeInbound(publish);

        // 修复验证点1：PUBLISH 存储等待 PUBREL 期间 payload 被 retain（refCnt=1），未被 autoRelease 释放
        assertEquals(1, payload.refCnt());
        assertEquals(1, client.getQos2PendingIncomingPublishes().size());
        // QoS2 第一阶段不应调用业务 handler
        assertTrue(received.isEmpty());

        MqttMessage pubrel = new MqttMessage(
                new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(1));
        channel.writeInbound(pubrel);

        // 修复验证点2：PUBREL 后 handler 收到完整 payload 内容（旧实现此处 use-after-free）
        assertEquals(1, received.size());
        assertEquals("qos2-payload", received.get(0));
        // 修复验证点3：retain/release 一一配对，最终 refCnt=0 无泄漏；pending 队列清空
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

        // 旧实现 invokeHandlersForIncomingPublish 末尾多余 release 与 autoRelease 叠加，
        // 双重释放抛 IllegalReferenceCountException，writeInbound 直接失败
        channel.writeInbound(publish);

        assertEquals(1, received.size());
        assertEquals("qos0-payload", received.get(0));
        // autoRelease 唯一释放，恰好归零
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
