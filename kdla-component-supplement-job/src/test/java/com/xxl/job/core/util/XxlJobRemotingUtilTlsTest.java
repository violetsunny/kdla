package com.xxl.job.core.util;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.xxl.job.core.biz.model.ReturnT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** CWE-295 回归：自签证书服务端必须握手失败。trust-all 存在时本测试返回 200 而失败。 */
class XxlJobRemotingUtilTlsTest {

    private static final char[] PASS = "changeit".toCharArray();

    private static HttpsServer server;
    private static Path tempDir;
    private static int port;

    @BeforeAll
    static void startSelfSignedServer() throws Exception {
        tempDir = Files.createTempDirectory("kdla-tls");
        Path keystore = tempDir.resolve("selfsigned.p12");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
        assertThat(Files.isExecutable(keytool)).as("keytool 必须可用").isTrue();

        Process p = new ProcessBuilder(keytool.toString(),
                "-genkeypair", "-alias", "tls", "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "1", "-dname", "CN=localhost",
                "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                "-keystore", keystore.toString(), "-storetype", "PKCS12",
                "-storepass", new String(PASS), "-keypass", new String(PASS))
                .redirectErrorStream(true).start();
        assertThat(p.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(p.exitValue()).as("keytool 生成自签证书失败").isZero();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keystore)) {
            ks.load(in, PASS);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, PASS);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);

        server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(ctx));
        server.createContext("/", exchange -> {
            byte[] body = "{\"code\":200}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) {
            server.stop(0);
        }
        if (tempDir != null) {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        }
    }

    @Test
    void postBody_rejectsUntrustedSelfSignedCertificate() {
        ReturnT result = XxlJobRemotingUtil.postBody(
                "https://localhost:" + port + "/api/test", "", 3, "", String.class);

        assertThat(result.getCode()).isEqualTo(ReturnT.FAIL_CODE);
        assertThat(result.getMsg()).contains("remoting error");
    }
}
