package top.kdla.framework.supplement.netty;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全审计 D-08 回归测试：NettyHttpServer 加载 truststore 却不强制客户端证书，mTLS 静默失效。
 *
 * <p>修复后 setNeedClientAuth(true)，无证书客户端被拒连（mTLS 语义）。
 *
 * <p>无修复则失败：旧代码 needClientAuth=false，truststore 从未用于校验。
 */
class NettyHttpServerMtlsTest {

    @TempDir
    Path tempDir;

    @Test
    void sslInitializer_requiresClientCertificate() throws Exception {
        File keyStore = newEmptyJks("server.jks");
        Class<?> initializerClass = Class.forName(
                "top.kdla.framework.supplement.netty.NettyHttpServer$HttpServerInitializer");
        Constructor<?> constructor = initializerClass.getDeclaredConstructor(
                boolean.class, File.class, File.class, String.class, NettyHttpServer.DispatcherServer.class);
        constructor.setAccessible(true);
        Object initializer = constructor.newInstance(true, keyStore, keyStore, "changeit",
                (NettyHttpServer.DispatcherServer<Object>) (key, t) -> null);
        Method initChannel = initializerClass.getDeclaredMethod("initChannel", SocketChannel.class);
        initChannel.setAccessible(true);
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        NioSocketChannel channel = new NioSocketChannel();
        try {
            group.register(channel).sync();
            initChannel.invoke(initializer, channel);
            SslHandler handler = (SslHandler) channel.pipeline().get("ssl");
            assertTrue(handler.engine().getNeedClientAuth(), "truststore语义即mTLS，须强制客户端证书");
            assertFalse(handler.engine().getUseClientMode());
        } finally {
            channel.close().sync();
            group.shutdownGracefully();
        }
    }

    private File newEmptyJks(String name) throws Exception {
        File file = tempDir.resolve(name).toFile();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            keyStore.store(out, "changeit".toCharArray());
        }
        return file;
    }
}
