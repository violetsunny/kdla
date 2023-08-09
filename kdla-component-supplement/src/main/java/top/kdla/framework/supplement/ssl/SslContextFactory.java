/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.ssl;

import cn.hutool.core.io.FileUtil;
import org.springframework.beans.factory.annotation.Value;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.security.KeyStore;

/**
 * @author kanglele
 * @version $Id: SslContextFactory, v 0.1 2023/6/13 17:06 kanglele Exp $
 */
public class SslContextFactory {

    private static final String PROTOCOL = "TLS"; // TODO: which protocols will be adopted?

    public SSLContext getServerContext(File server,File servertruststore,String keyStorePassword){
        SSLContext serverContext = null;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            //ts.load(SslContextFactory.class.getClassLoader().getResourceAsStream("ca\\certs"+active+"\\server.keystore"), keyStorePassword.toCharArray());
            ks.load(FileUtil.getInputStream(server), keyStorePassword.toCharArray());
            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());
            // truststore
            KeyStore ts = KeyStore.getInstance("JKS");
            //ts.load(SslContextFactory.class.getClassLoader().getResourceAsStream("ca\\certs"+active+"\\servertruststore.keystore"), keyStorePassword.toCharArray());
            ts.load(FileUtil.getInputStream(servertruststore), keyStorePassword.toCharArray());
            // set up trust manager factory to use our trust store
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            // Initialize the SSLContext to work with our key managers.
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        }
        return serverContext;
    }

    public SSLContext getClientContext(File client,File clienttruststore,String keyStorePassword){
        SSLContext clientContext = null;
        try {
            // keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            //ks.load(SslContextFactory.class.getClassLoader().getResourceAsStream("ca\\certs"+active+"\\client.keystore"), keyStorePassword.toCharArray());
            ks.load(FileUtil.getInputStream(client), keyStorePassword.toCharArray());
            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());
            // truststore
            KeyStore ts = KeyStore.getInstance("JKS");
            //ts.load(SslContextFactory.class.getClassLoader().getResourceAsStream("ca\\certs"+active+"\\clienttruststore.keystore"), keyStorePassword.toCharArray());
            ks.load(FileUtil.getInputStream(clienttruststore), keyStorePassword.toCharArray());
            // set up trust manager factory to use our trust store
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            throw new Error("Failed to initialize the client-side SSLContext", e);
        }
        return clientContext;
    }

}
