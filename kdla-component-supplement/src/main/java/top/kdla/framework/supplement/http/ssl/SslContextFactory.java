/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.http.ssl;

import cn.hutool.core.io.FileUtil;
import io.netty.handler.ssl.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * @author kanglele
 * @version $Id: SslContextFactory, v 0.1 2023/6/13 17:06 kanglele Exp $
 */
public class SslContextFactory {

    private static final String PROTOCOL = "TLS"; // TODO: which protocols will be adopted?

    /**
     * @param server           服务器端的密钥库文件。密钥库文件包含服务器的证书和私钥。这个文件通常是Java KeyStore（JKS）格式的，扩展名为.keystore或.jks。
     * @param servertruststore 服务器端的信任库文件。信任库文件包含受信任的证书颁发机构（CA）证书。服务器使用这些证书来验证客户端证书。这个文件通常也是Java KeyStore（JKS）格式的，扩展名为.truststore或.jks。
     * @param keyStorePassword 密钥库文件的密码。在加载密钥库时，需要提供密码以解密存储在其中的私钥。
     * @return
     */
    public static SSLContext getServerContext(File server, File servertruststore, String keyStorePassword) {
        SSLContext serverContext = null;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream ksIs = FileUtil.getInputStream(server)) {
                ks.load(ksIs, keyStorePassword.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());
            KeyStore ts = KeyStore.getInstance("JKS");
            try (InputStream tsIs = FileUtil.getInputStream(servertruststore)) {
                ts.load(tsIs, keyStorePassword.toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize the server-side SSLContext", e);
        }
        return serverContext;
    }

    public static SslContext forServer(File server, File servertruststore, String keyStorePassword) {
        return toNettySslContext(getServerContext(server, servertruststore, keyStorePassword), false);
    }

    //产物默认不含域名校验，消费方须对SslHandler.engine()设endpointIdentificationAlgorithm("HTTPS")
    public static SslContext toNettySslContext(SSLContext javaxSslContext, boolean isClient) {
        return new JdkSslContext(javaxSslContext, isClient, isClient ? ClientAuth.NONE : ClientAuth.REQUIRE);
    }

    /**
     * @param client           客户端的密钥库文件，包含客户端的证书和私钥。
     * @param clienttruststore 客户端的信任库文件，包含受信任的证书颁发机构（CA）证书。客户端使用这些证书来验证服务器证书。
     * @param keyStorePassword 客户端密钥库文件的密码。
     * @return
     */
    public static SSLContext getClientContext(File client, File clienttruststore, String keyStorePassword) {
        SSLContext clientContext = null;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream ksIs = FileUtil.getInputStream(client)) {
                ks.load(ksIs, keyStorePassword.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());
            KeyStore ts = KeyStore.getInstance("JKS");
            try (InputStream tsIs = FileUtil.getInputStream(clienttruststore)) {
                ts.load(tsIs, keyStorePassword.toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize the client-side SSLContext", e);
        }
        return clientContext;
    }

    public static SslContext forClient(File client, File clienttruststore, String keyStorePassword) {
        return toNettySslContext(getClientContext(client, clienttruststore, keyStorePassword), true);
    }
}
