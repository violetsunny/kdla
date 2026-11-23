package top.kdla.framework.supplement.mqtt.netty;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 H7 回归测试：MqttPingHandler 心跳超时判断反了。
 *
 * <p>旧实现 sendPingReq 里 if (this.pingRespTimeout != null) 才调度超时任务，
 * 首次写空闲时该字段必为 null，超时任务永不调度，对端失联也不发 DISCONNECT。
 *
 * <p>修复后改为 == null 时调度；收到 PINGRESP 取消超时；重复写空闲不重复调度。
 *
 * <p>无修复则失败：旧代码推进虚拟时间后无 DISCONNECT 写出，连接不关闭。
 */
class MqttPingHandlerTimeoutTest {

    private static MqttMessage message(MqttMessageType type) {
        return new MqttMessage(new MqttFixedHeader(type, false, MqttQoS.AT_MOST_ONCE, false, 0));
    }

    @Test
    void writerIdle_noPingResp_disconnectAfterKeepalive() {
        EmbeddedChannel channel = new EmbeddedChannel(new MqttPingHandler(1));
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.WRITER_IDLE_STATE_EVENT);
        MqttMessage pingReq = channel.readOutbound();
        assertEquals(MqttMessageType.PINGREQ, pingReq.fixedHeader().messageType());

        channel.advanceTimeBy(1, TimeUnit.SECONDS);
        channel.runScheduledPendingTasks();

        MqttMessage disconnect = channel.readOutbound();
        assertEquals(MqttMessageType.DISCONNECT, disconnect.fixedHeader().messageType());
        assertFalse(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void pingRespReceived_timeoutCancelled() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(new MqttPingHandler(1));
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.WRITER_IDLE_STATE_EVENT);
        MqttMessage pingReq = channel.readOutbound();
        assertEquals(MqttMessageType.PINGREQ, pingReq.fixedHeader().messageType());

        channel.writeInbound(message(MqttMessageType.PINGRESP));
        channel.advanceTimeBy(1, TimeUnit.SECONDS);
        channel.runScheduledPendingTasks();

        assertNull(channel.readOutbound());
        assertTrue(channel.isActive());
        channel.finishAndReleaseAll();
    }

    @Test
    void repeatedWriterIdle_timeoutScheduledOnce() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(new MqttPingHandler(1));
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.WRITER_IDLE_STATE_EVENT);
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.WRITER_IDLE_STATE_EVENT);
        assertEquals(MqttMessageType.PINGREQ, ((MqttMessage) channel.readOutbound()).fixedHeader().messageType());
        assertEquals(MqttMessageType.PINGREQ, ((MqttMessage) channel.readOutbound()).fixedHeader().messageType());

        channel.advanceTimeBy(1, TimeUnit.SECONDS);
        channel.runScheduledPendingTasks();

        assertEquals(MqttMessageType.DISCONNECT, ((MqttMessage) channel.readOutbound()).fixedHeader().messageType());
        assertNull(channel.readOutbound());
        channel.finishAndReleaseAll();
    }
}