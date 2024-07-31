/**
 * LY.com Inc.
 * Copyright (c) 2004-2024 All Rights Reserved.
 */
package top.kdla.framework.supplement.http;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author kanglele
 * @version $Id: ProtoHttpClient, v 0.1 2024/7/31 下午3:32 kanglele Exp $
 */
public class ProtoHttpClient {

    @Value("${http.connectReqTimeout:1000}")
    private int connectReqTimeout;

    private final HttpClient protoClient;

    public ProtoHttpClient(HttpClient protoClient) {
        this.protoClient = protoClient;
    }

    public CompletableFuture<HttpResponse<String>> sendRequest(String method, String url, Map<String, String> header, Object req) {
        Map<String, String> headers;
        if (header != null) {
            //指定为小写
            headers = header.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
        } else {
            headers = new HashMap<>();
        }
        headers.putIfAbsent(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE);

        if ("GET".equalsIgnoreCase(method)) {
            return sendGetRequest(url, headers);
        } else {
            return sendPostRequest(url, headers, req instanceof String reqq ? reqq : JSON.toJSONString(req));
        }
    }

    private CompletableFuture<HttpResponse<String>> sendGetRequest(String uri, Map<String, String> headers) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofMillis(connectReqTimeout))
                .GET();

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest request = requestBuilder.build();
        return protoClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private CompletableFuture<HttpResponse<String>> sendPostRequest(String uri, Map<String, String> headers, String body) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofMillis(connectReqTimeout))
                .POST(HttpRequest.BodyPublishers.ofString(body));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest request = requestBuilder.build();
        return protoClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

}
