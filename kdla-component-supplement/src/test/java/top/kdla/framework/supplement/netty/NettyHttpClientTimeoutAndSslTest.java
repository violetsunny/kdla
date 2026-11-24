package top.kdla.framework.supplement.netty;

import com.sun.net.httpserver.HttpServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 C6+H10 回归测试：NettyHttpClient 超时缺失与 SSL 配置失效。
 *
 * <p>旧实现缺陷：
 * (1) connectTimeout 用 @Value 但该类非 Spring Bean，永不注入恒 0；
 * (2) 响应等待 future.get() 无超时，对端不回包且不断连时永久挂起调用线程；
 * (3) setSsl 只改字段不重建 initializer，mTLS/https 配置不生效；
 * (4) https 未提供 keystore 时传 null 给 SslContextFactory 必 NPE。
 *
 * <p>修复后：connectTimeout 默认 3000；requestTimeout 默认 30000 且可设；
 * setSsl 重建 handler；无证书走 SSLContext.getDefault()。
 *
 * <p>无修复则失败：旧代码 connectTimeout==0、无响应时 10 秒内不返回、
 * setSsl 后 initializer.ssl 仍 false、无证书 initChannel 抛异常。
 */
class NettyHttpClientTimeoutAndSslTest {

    @Test
    void connectTimeout_hasExplicitDefault() throws Exception {
        NettyHttpClient client = new NettyHttpClient("http://127.0.0.1", false);
        try {
            Field f = NettyHttpClient.class.getDeclaredField("connectTimeout");
            f.setAccessible(true);
            assertEquals(3000, f.getInt(client));
        } finally {
            client.close();
        }
    }

    @Test
    void setSsl_rebuildsInitializerWithSslEnabled() throws Exception {
        NettyHttpClient client = new NettyHttpClient("http://127.0.0.1", false);
        try {
            client.setSsl(null, null, null);
            Field bootstrapField = NettyHttpClient.class.getDeclaredField("bootstrap");
            bootstrapField.setAccessible(true);
            Bootstrap bootstrap = (Bootstrap) bootstrapField.get(client);
            Object initializer = bootstrap.config().handler();
            Field sslField = initializer.getClass().getDeclaredField("ssl");
            sslField.setAccessible(true);
            assertTrue(sslField.getBoolean(initializer));
        } finally {
            client.close();
        }
    }

    @Test
    void sslInitializer_withoutKeystore_installsSslHandlerWithHostnameVerification() throws Exception {
        Class<?> initializerClass = Class.forName(
                "top.kdla.framework.supplement.netty.NettyHttpClient$HttpClientInitializer");
        Constructor<?> constructor = initializerClass.getDeclaredConstructor(
                boolean.class, File.class, File.class, String.class, String.class, int.class);
        constructor.setAccessible(true);
        Object initializer = constructor.newInstance(true, null, null, null, "example.com", 443);
        Method initChannel = initializerClass.getDeclaredMethod("initChannel", SocketChannel.class);
        initChannel.setAccessible(true);
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        NioSocketChannel channel = new NioSocketChannel();
        try {
            group.register(channel).sync();
            initChannel.invoke(initializer, channel);
            SslHandler handler = (SslHandler) channel.pipeline().get("ssl");
            assertInstanceOf(SslHandler.class, handler);
            //安全审计D-04：JDK SSLEngine默认不做域名校验，须显式开启HTTPS端点识别
            assertEquals("HTTPS", handler.engine().getSSLParameters().getEndpointIdentificationAlgorithm(),
                    "须开启HTTPS端点识别防证书域名不匹配MITM");
        } finally {
            channel.close().sync();
            group.shutdownGracefully();
        }
    }

    @Test
    void constructor_capturesPeerHostPortForTls() throws Exception {
        NettyHttpClient client = new NettyHttpClient("https://127.0.0.1:8443", false);
        try {
            Field bootstrapField = NettyHttpClient.class.getDeclaredField("bootstrap");
            bootstrapField.setAccessible(true);
            Bootstrap bootstrap = (Bootstrap) bootstrapField.get(client);
            Object initializer = bootstrap.config().handler();
            Field hostField = initializer.getClass().getDeclaredField("peerHost");
            hostField.setAccessible(true);
            assertEquals("127.0.0.1", hostField.get(initializer));
            Field portField = initializer.getClass().getDeclaredField("peerPort");
            portField.setAccessible(true);
            assertEquals(8443, portField.getInt(initializer));
        } finally {
            client.close();
        }
    }

    @Test
    void sendRequest_peerSilent_throwsTimeoutInsteadOfHang() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            Thread acceptor = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    InputStream in = socket.getInputStream();
                    while (in.read() != -1) {
                    }
                } catch (Exception ignored) {
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
            String url = "http://127.0.0.1:" + server.getLocalPort();
            NettyHttpClient client = new NettyHttpClient(url, false);
            client.setRequestTimeout(500);
            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThrows(TimeoutException.class, () ->
                            client.sendRequestPromise(url + "/x", null, "GET", new HashMap<>())));
        }
    }

    @Test
    void sendRequest_normalResponse_bodyFullyReturned() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/hello", exchange -> {
            byte[] body = "hello-body".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hello";
            NettyHttpClient client = new NettyHttpClient(url, false);
            String response = assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    client.sendRequestCompletable(url, null, "GET", new HashMap<>()));
            assertEquals("hello-body", response);
        } finally {
            server.stop(0);
        }
    }
}