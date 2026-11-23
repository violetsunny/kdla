package top.kdla.framework.supplement.netty;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.channel.EventLoopGroup;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 C5 回归测试：NettyHttpServer 启动即关端口、绑定失败不可见。
 *
 * <p>旧实现两处缺陷：
 * (1) start() 的 finally 立即 f.channel().close()，服务端口瞬开即关，外部永远连不上；
 * (2) 绑定失败（如端口被占用）不记录错误日志，失败静默不可见。
 *
 * <p>修复后：端口常开，仅 close() 关闭；绑定失败记 ERROR 并释放线程资源。
 *
 * <p>无修复则失败：旧代码 start 后端口探测 5 秒内始终连不上；占用端口启动无「绑定失败」ERROR 日志。
 */
class NettyHttpServerLifecycleTest {

    @Test
    void start_portStaysOpenUntilClose() throws Exception {
        int port = freePort();
        NettyHttpServer server = new NettyHttpServer((key, t) -> "ok", false);
        server.start(port);
        try {
            awaitPortOpen(port);
        } finally {
            server.close();
        }
        awaitPortClosed(port);
    }

    @Test
    void start_portOccupied_logsErrorAndReleasesGroups() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0, 50)) {
            int port = occupied.getLocalPort();
            Logger logger = (Logger) LoggerFactory.getLogger(NettyHttpServer.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                NettyHttpServer server = new NettyHttpServer((key, t) -> "ok", false);
                server.start(port);
                awaitBindFailureLog(appender);
                assertTrue(groupShuttingDown(server, "bossGroup"));
                assertTrue(groupShuttingDown(server, "workerGroup"));
            } finally {
                logger.detachAppender(appender);
            }
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void awaitPortOpen(int port) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 200);
                return;
            } catch (IOException e) {
                Thread.sleep(50);
            }
        }
        throw new AssertionError("start后5秒内端口" + port + "始终无法连接");
    }

    private static void awaitPortClosed(int port) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 200);
                Thread.sleep(50);
            } catch (IOException e) {
                return;
            }
        }
        throw new AssertionError("close后5秒内端口" + port + "仍可连接");
    }

    private static void awaitBindFailureLog(ListAppender<ILoggingEvent> appender) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            boolean found = appender.list.stream().anyMatch(e ->
                    e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("绑定失败"));
            if (found) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("端口占用启动后5秒内未记录「绑定失败」ERROR日志");
    }

    private static boolean groupShuttingDown(NettyHttpServer server, String fieldName) throws Exception {
        Field f = NettyHttpServer.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return ((EventLoopGroup) f.get(server)).isShuttingDown();
    }
}