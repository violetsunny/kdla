package top.kdla.framework.supplement.mqtt.netty;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 安全审计 D-05 回归测试：MQTT TLS 路径 newHandler 带 peer 但 netty 默认不开域名校验。
 *
 * <p>修复后 initChannel 对 SslHandler.engine() 显式设 endpointIdentificationAlgorithm("HTTPS")。
 *
 * <p>无修复则失败：旧代码 engine 的 endpointIdentificationAlgorithm 为 null，
 * 任意 CA 合法证书即可 MITM 窃取 CONNECT 凭据。
 */
class MqttClientImplTlsHostnameTest {

    @Test
    void tlsInitializer_enablesHostnameVerification() throws Exception {
        MqttClientImpl client = new MqttClientImpl(null);
        SslContext sslContext = SslContextBuilder.forClient().build();
        Promise<MqttConnectResult> connectFuture = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        Class<?> initializerClass = Class.forName(
                "top.kdla.framework.supplement.mqtt.netty.MqttClientImpl$MqttChannelInitializer");
        Constructor<?> constructor = initializerClass.getDeclaredConstructor(
                MqttClientImpl.class, Promise.class, String.class, int.class, SslContext.class);
        constructor.setAccessible(true);
        Object initializer = constructor.newInstance(client, connectFuture, "broker.example.com", 8883, sslContext);
        Method initChannel = initializerClass.getDeclaredMethod("initChannel", SocketChannel.class);
        initChannel.setAccessible(true);
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        NioSocketChannel channel = new NioSocketChannel();
        try {
            group.register(channel).sync();
            initChannel.invoke(initializer, channel);
            SslHandler handler = (SslHandler) channel.pipeline().first();
            assertInstanceOf(SslHandler.class, handler);
            assertEquals("HTTPS", handler.engine().getSSLParameters().getEndpointIdentificationAlgorithm(),
                    "MQTT TLS须开启HTTPS端点识别防MITM");
        } finally {
            channel.close().sync();
            group.shutdownGracefully();
        }
    }
}
