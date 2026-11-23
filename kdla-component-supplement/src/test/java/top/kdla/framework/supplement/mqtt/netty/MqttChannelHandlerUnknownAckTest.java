package top.kdla.framework.supplement.mqtt.netty;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 H8 回归测试：MqttChannelHandler 重复/错序 PUBREC、PUBCOMP 空指针。
 *
 * <p>旧实现 handlePubrec/handlePubcomp 取 pendingPublish 后不判空直接调方法，
 * 重复或错序 ACK（对应 publish 已完成移除）时 NPE 杀死 event loop。
 *
 * <p>修复后对齐 handlePuback 判空提前 return。
 *
 * <p>无修复则失败：旧代码 writeInbound 未知 packetId 的 PUBREC/PUBCOMP 抛 NPE。
 *
 * <p>注：清单所述 handleConack default 分支不可达——MqttConnectReturnCode 仅 6 个枚举值
 * 且 switch 已全覆盖，无法构造未知 returnCode，故不测该分支。
 */
class MqttChannelHandlerUnknownAckTest {

    private static MqttMessage ackMessage(MqttMessageType type, int packetId) {
        return new MqttMessage(
                new MqttFixedHeader(type, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(packetId));
    }

    @Test
    void unknownPubrec_ignoredWithoutNpe() {
        MqttClientImpl client = new MqttClientImpl(null);
        Promise<MqttConnectResult> connectFuture = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        EmbeddedChannel channel = new EmbeddedChannel(new MqttChannelHandler(client, connectFuture));
        channel.readOutbound();

        channel.writeInbound(ackMessage(MqttMessageType.PUBREC, 999));

        assertNull(channel.readOutbound());
        assertTrue(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void unknownPubcomp_ignoredWithoutNpe() {
        MqttClientImpl client = new MqttClientImpl(null);
        Promise<MqttConnectResult> connectFuture = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        EmbeddedChannel channel = new EmbeddedChannel(new MqttChannelHandler(client, connectFuture));
        channel.readOutbound();

        channel.writeInbound(ackMessage(MqttMessageType.PUBCOMP, 999));

        assertNull(channel.readOutbound());
        assertTrue(channel.isActive());
        channel.finishAndReleaseAll();
    }
}