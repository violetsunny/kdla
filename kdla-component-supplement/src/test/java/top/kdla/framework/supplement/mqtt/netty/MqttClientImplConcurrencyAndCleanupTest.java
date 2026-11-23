package top.kdla.framework.supplement.mqtt.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;
import top.kdla.framework.supplement.mqtt.MqttHandler;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 H9 回归测试：MqttClientImpl 并发容器、QoS2 堆外内存泄漏、eventLoop 生命周期。
 *
 * <p>旧实现三处缺陷：
 * (1) serverSubscriptions/pendingSubscribeTopics 用 HashSet、subscriptions/handlerToSubscription
 *     用裸 HashMultimap，调用线程与 event loop 并发读写丢更新/CME；
 * (2) 连接关闭时 qos2PendingIncomingPublishes 直接 clear，存储时 retain 的 payload 未 release，堆外内存泄漏；
 * (3) disconnect 不关闭自建 eventLoop（线程泄漏）；修复后自建才关，外部注入的不关。
 *
 * <p>无修复则失败：旧代码类型断言失败、并发 put 后 size<4000、断连后 refCnt 仍为 1、
 * disconnect 后自建 eventLoop 未 shuttingDown。
 */
class MqttClientImplConcurrencyAndCleanupTest {

    @Test
    void sharedCollections_areConcurrentContainers() {
        MqttClientImpl client = new MqttClientImpl(null);
        assertInstanceOf(ConcurrentHashMap.KeySetView.class, client.getServerSubscriptions());
        assertInstanceOf(ConcurrentHashMap.KeySetView.class, client.getPendingSubscribeTopics());
        assertTrue(client.getSubscriptions().getClass().getName().contains("Synchronized"));
        assertTrue(client.getHandlerToSubscription().getClass().getName().contains("Synchronized"));
        assertInstanceOf(ConcurrentHashMap.class, client.getQos2PendingIncomingPublishes());
        assertInstanceOf(ConcurrentHashMap.class, client.getPendingPublishes());
    }

    @Test
    void concurrentSubscribe_noLostUpdate() throws Exception {
        MqttClientImpl client = new MqttClientImpl(null);
        MqttHandler handler = new MqttHandler() {
            @Override
            public java.util.regex.Pattern topicPattern() {
                return java.util.regex.Pattern.compile(".*");
            }

            @Override
            public void onMessage(String topic, ByteBuf payload) {
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(8);
        try {
            for (int t = 0; t < 8; t++) {
                int base = t * 500;
                pool.execute(() -> {
                    try {
                        for (int i = 0; i < 500; i++) {
                            String topic = "t/" + (base + i);
                            client.getSubscriptions().put(topic, new MqttSubscription(topic, handler, false));
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertTrue(done.await(30, TimeUnit.SECONDS));
            assertEquals(4000, client.getSubscriptions().size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void qos2PendingPublish_releasedOnChannelClose() throws Exception {
        MqttClientConfig config = new MqttClientConfig();
        config.setReconnect(false);
        config.setProtocolVersion(MqttVersion.MQTT_3_1_1);
        MqttClientImpl client = new MqttClientImpl(config, null);

        byte[] publishBytes = encodeQos2Publish("t/leak", 7);
        CountDownLatch stored = new CountDownLatch(1);
        AtomicReference<Throwable> brokerError = new AtomicReference<>();
        ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread broker = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                socket.getInputStream().read(new byte[512]);
                OutputStream out = socket.getOutputStream();
                out.write(new byte[]{0x20, 0x02, 0x00, 0x00});
                out.write(publishBytes);
                out.flush();
                stored.await(10, TimeUnit.SECONDS);
            } catch (Throwable e) {
                brokerError.set(e);
            }
        });
        broker.start();
        try {
            client.connect("127.0.0.1", serverSocket.getLocalPort());
            MqttIncomingQos2Publish pending = awaitQos2Stored(client);
            //MqttPublishMessage.refCnt()内部ensureAccessible，归零后再调会抛异常，须提前取出原始ByteBuf
            ByteBuf payload = pending.getIncomingPublish().payload();
            awaitRefCnt(payload, 1);
            stored.countDown();
            awaitQos2Cleared(client);
            assertEquals(0, payload.refCnt());
        } finally {
            stored.countDown();
            client.disconnect();
            serverSocket.close();
            broker.join(5000);
        }
        assertNull(brokerError.get());
        assertTrue(client.getEventLoop().isShuttingDown());
    }

    @Test
    void disconnect_externalEventLoop_notShutDown() {
        MqttClientImpl client = new MqttClientImpl(null);
        EventLoopGroup external = new NioEventLoopGroup(1);
        try {
            client.setEventLoop(external);
            client.disconnect();
            assertFalse(external.isShuttingDown());
        } finally {
            external.shutdownGracefully();
        }
    }

    private static byte[] encodeQos2Publish(String topic, int packetId) {
        EmbeddedChannel encoder = new EmbeddedChannel(MqttEncoder.INSTANCE);
        MqttPublishMessage message = new MqttPublishMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.EXACTLY_ONCE, false, 0),
                new MqttPublishVariableHeader(topic, packetId),
                Unpooled.copiedBuffer("leak-payload", CharsetUtil.UTF_8));
        encoder.writeOutbound(message);
        ByteBuf encoded = encoder.readOutbound();
        byte[] bytes = new byte[encoded.readableBytes()];
        encoded.readBytes(bytes);
        encoded.release();
        encoder.finishAndReleaseAll();
        return bytes;
    }

    private static MqttIncomingQos2Publish awaitQos2Stored(MqttClientImpl client) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            MqttIncomingQos2Publish pending = client.getQos2PendingIncomingPublishes().get(7);
            if (pending != null) {
                return pending;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("10秒内未收到QoS2 PUBLISH存储");
    }

    //retain发生在channelRead0内，autoRelease在其返回后，须等引用计数落定再断言
    private static void awaitRefCnt(ByteBuf payload, int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (payload.refCnt() == expected) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("10秒内refCnt未落定为" + expected + "，实际:" + payload.refCnt());
    }

    private static void awaitQos2Cleared(MqttClientImpl client) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (client.getQos2PendingIncomingPublishes().isEmpty()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("断连后10秒内qos2PendingIncomingPublishes未清空");
    }
}