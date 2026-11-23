package top.kdla.framework.supplement.netty;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 清单#主题7 回归测试：NettyHttpClient 端口解析。
 *
 * <p>旧实现两处缺陷：
 * (1) IP 分支未显式指定端口时 getPort() 返回 -1，createUnresolved(host,-1) 抛 "port out of range"，
 *     导致 new NettyHttpClient("http://127.0.0.1", false) 直接构造失败；
 * (2) 域名分支忽略 URL 显式端口，强制 80/443，导致 "http://host:8080" 误连 80。
 *
 * <p>修复后：优先取 URL 显式端口，缺省按协议取默认端口(http=80/https=443)。
 *
 * <p>无修复则失败：旧代码构造抛 "port out of range"，且 getInetAddress 返回端口为 80 而非 8080。
 */
class NettyHttpClientPortTest {

    @Test
    void constructor_ipWithoutExplicitPort_noPortOutOfRange() {
        // 旧实现 createUnresolved(host,-1) 会抛 IllegalArgumentException("port out of range: -1")
        assertDoesNotThrow(() -> {
            NettyHttpClient client = new NettyHttpClient("http://127.0.0.1", false);
            client.close();
        });
    }

    @Test
    void getInetAddress_domainWithExplicitPort_preserved() throws Exception {
        NettyHttpClient client = new NettyHttpClient("http://127.0.0.1", false);
        try {
            Method m = NettyHttpClient.class.getDeclaredMethod("getInetAddress", String.class);
            m.setAccessible(true);
            InetSocketAddress addr = (InetSocketAddress) m.invoke(client, "http://localhost:8080");
            // 修复验证点：URL 显式端口 8080 被保留，旧实现会被强制改为 80
            assertEquals(8080, addr.getPort());
        } finally {
            client.close();
        }
    }
}