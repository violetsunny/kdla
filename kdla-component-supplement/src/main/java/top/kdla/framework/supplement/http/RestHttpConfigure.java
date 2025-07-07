package top.kdla.framework.supplement.http;


import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Configuration
@Slf4j
public class RestHttpConfigure {

    @Value("${http.connectionPoolSize:200}")
    private int connectionPoolSize;

    @Value("${http.socketTimeout:3000}")
    private int socketTimeout;

    @Value("${http.connectTimeout:1000}")
    private int connectTimeout;

    @Value("${http.connectReqTimeout:1000}")
    private int connectReqTimeout;

    @Bean
    @ConditionalOnMissingBean(BaseHttpClient.class)
    public BaseHttpClient baseHttpClient(RestTemplate restTemplate) {
        if (log.isInfoEnabled()) {
            log.info("BaseHttpClient init Bean");
        }
        return new BaseHttpClient(restTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate(ClientHttpRequestFactory httpRequestFactory) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(httpRequestFactory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        return restTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(ClientHttpRequestFactory.class)
    public ClientHttpRequestFactory httpRequestFactory(HttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Bean
    @ConditionalOnMissingBean(HttpClient.class)
    public HttpClient httpClient() {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        //设置整个连接池最大连接数 根据自己的场景决定
        connectionManager.setMaxTotal(connectionPoolSize);
        //路由是对maxTotal的细分
        connectionManager.setDefaultMaxPerRoute(100);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout,TimeUnit.MILLISECONDS)//连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
                .setConnectionRequestTimeout(connectReqTimeout,TimeUnit.MILLISECONDS)//从连接池中获取连接的超时时间，超过该时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
                .setResponseTimeout(socketTimeout,TimeUnit.MILLISECONDS) //服务器返回数据(response)的时间，超过该时间抛出read timeout
                .build();
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(java.net.http.HttpClient.class)
    public java.net.http.HttpClient protoClient() {
        return java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ProtoHttpClient.class)
    public ProtoHttpClient protoHttpClient(java.net.http.HttpClient protoClient) {
        return new ProtoHttpClient(protoClient);
    }
}
