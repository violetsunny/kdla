package top.kdla.framework.supplement.http.ssl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 清单 M4 回归测试：SslContextFactory 初始化失败抛 java.lang.Error。
 *
 * <p>旧实现缺陷：密钥库缺失/损坏时 catch 块 throw new Error(...)，
 * Error 不属于业务异常体系，调用方的 catch(Exception) 与统一异常处理都拦不住，
 * 直接击穿线程；且未保留原始 cause 之外的可诊断类型。
 *
 * <p>修复后：统一抛 IllegalStateException 并携带 cause，可被常规异常处理捕获。
 *
 * <p>无修复则失败：旧代码抛 Error，assertThrows(IllegalStateException) 无法匹配，测试直接报错。
 */
class SslContextFactoryTest {

    private static final String PASSWORD = "changeit";

    @TempDir
    Path tempDir;

    @Test
    void serverContext_missingKeyStore_throwsIllegalStateException() {
        File missing = tempDir.resolve("absent-server.jks").toFile();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SslContextFactory.getServerContext(missing, missing, PASSWORD));
        assertNotNull(ex.getCause(), "须保留原始异常cause");
    }

    @Test
    void clientContext_missingKeyStore_throwsIllegalStateException() {
        File missing = tempDir.resolve("absent-client.jks").toFile();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SslContextFactory.getClientContext(missing, missing, PASSWORD));
        assertNotNull(ex.getCause(), "须保留原始异常cause");
    }

    @Test
    void validKeyStore_buildsTlsContexts() throws Exception {
        File keyStore = newEmptyJks("valid.jks");

        SSLContext server = SslContextFactory.getServerContext(keyStore, keyStore, PASSWORD);
        assertNotNull(server);
        assertEquals("TLS", server.getProtocol());

        SSLContext client = SslContextFactory.getClientContext(keyStore, keyStore, PASSWORD);
        assertNotNull(client);
        assertEquals("TLS", client.getProtocol());
    }

    private File newEmptyJks(String name) throws Exception {
        File file = tempDir.resolve(name).toFile();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            keyStore.store(out, PASSWORD.toCharArray());
        }
        return file;
    }
}
