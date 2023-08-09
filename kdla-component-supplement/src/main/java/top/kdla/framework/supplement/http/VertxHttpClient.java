/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.http;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import lombok.extern.slf4j.Slf4j;
import top.kdla.framework.common.utils.ObjectUtil;
import top.kdla.framework.dto.ErrorCode;
import top.kdla.framework.exception.BizException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author kanglele
 * @version $Id: VertxHttpUtil, v 0.1 2023/5/17 18:36 kanglele Exp $
 */
@Slf4j
public class VertxHttpClient {

    private final WebClient webClient;

    private static final String CONTENT_TYPE_JSON = "application/json";

    public VertxHttpClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * future.thenAccept(user -> {
     * System.out.println("Received user:\n" + user);
     * }).exceptionally(ex -> {
     * System.out.println("Something went wrong: " + ex.getMessage());
     * return null;
     * });
     *
     * @param url
     * @param headers
     * @param res
     * @param <T>
     * @return
     */
    public <T> CompletableFuture<T> getJson(String url, Optional<Map<String, String>> headers, Class<T> res) {
        CompletableFuture<T> future = new CompletableFuture<>();
        HttpRequest<Buffer> request = webClient.getAbs(url).putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), CONTENT_TYPE_JSON);
        headers.ifPresent(h -> request.putHeaders(HeadersMultiMap.httpHeaders().setAll(h)));
        request.send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                future.complete(response.bodyAsJson(res));
            } else {
                future.completeExceptionally(ar.cause());
            }
        });
        return future;
    }

    public <T> CompletableFuture<T> postJson(String url, Optional<Map<String, String>> headers, Object req, Class<T> res) {
        CompletableFuture<T> future = new CompletableFuture<>();
        HttpRequest<Buffer> request = webClient.postAbs(url).putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), CONTENT_TYPE_JSON);
        headers.ifPresent(h -> request.putHeaders(HeadersMultiMap.httpHeaders().setAll(h)));
        request.sendJson(req, ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                future.complete(response.bodyAsJson(res));
            } else {
                future.completeExceptionally(ar.cause());
            }
        });
        return future;
    }

    public <T> CompletableFuture<T> sendRequest(HttpMethod method, String url, Map<String, String> headers, Object req, Class<T> res) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Future<HttpResponse<Buffer>> responseFuture = createRequest(method, url, headers, req);
        responseFuture.onComplete(ar -> {
            if (ar.succeeded()) {
                try {
                    HttpResponse<Buffer> response = ar.result();
                    if (res.equals(String.class)) {
                        String result = response.bodyAsString();
                        future.complete((T) result);
                        log.info("VertxHttpClient-send url:{} req:{} result:{}", url, JSON.toJSONString(req), result);
                    } else {
                        T result = response.bodyAsJson(res);//默认json返回
                        future.complete(result);
                        log.info("VertxHttpClient-send url:{} req:{} result:{}", url, JSON.toJSONString(req), JSON.toJSONString(result));
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e.getCause());
                    throw new BizException(ErrorCode.FAIL.getCode(), "调用外部接口异常", e.getCause());
                }
            } else {
                future.completeExceptionally(ar.cause());
                throw new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败");
            }
        });
        return future;
    }

    /**
     * Get请求的参数，请直接在url后拼接，不走addQueryParam
     *
     * @param method
     * @param url
     * @param headers
     * @param req
     * @return
     */
    private Future<HttpResponse<Buffer>> createRequest(HttpMethod method, String url, Map<String, String> headers, Object req) {
        if (headers != null) {
            //指定为小写
            headers = headers.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
        } else {
            headers = new HashMap<>();
        }
        headers.putIfAbsent(HttpHeaderNames.CONTENT_TYPE.toString(), CONTENT_TYPE_JSON);

        HttpRequest<Buffer> request = null;
        //HTTP method
        if (HttpMethod.GET.equals(method)) {
            request = webClient.getAbs(url);
        } else if (HttpMethod.POST.equals(method)) {
            request = webClient.postAbs(url);
        } else if (HttpMethod.PUT.equals(method)) {
            request = webClient.putAbs(url);
        } else if (HttpMethod.PATCH.equals(method)) {
            request = webClient.patchAbs(url);
        } else if (HttpMethod.DELETE.equals(method)) {
            request = webClient.deleteAbs(url);
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        //HTTP head
        request.putHeaders(HeadersMultiMap.httpHeaders().setAll(headers));
        //HTTP head type responseFuture
        Future<HttpResponse<Buffer>> responseFuture;
        if (req != null) {
            String contentType = headers.get(HttpHeaderNames.CONTENT_TYPE.toString());
            if (contentType.equalsIgnoreCase(HttpHeaderValues.APPLICATION_JSON.toString())) {
                responseFuture = request.sendJson(req);
            } else if (contentType.equalsIgnoreCase(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
                responseFuture = request.sendForm((MultiMap) req);
            } else if (contentType.equalsIgnoreCase(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) {
                responseFuture = request.sendMultipartForm((MultipartForm) req);
            } else {
                byte[] data = ObjectUtil.ObjectToByte(req);
                responseFuture = request.sendBuffer(Buffer.buffer(data));
            }
        } else {
            responseFuture = request.send();
        }

        return responseFuture;
    }

}
