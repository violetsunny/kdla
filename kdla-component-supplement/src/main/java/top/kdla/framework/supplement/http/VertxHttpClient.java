/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.http;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import org.springframework.stereotype.Component;
import top.kdla.framework.common.utils.ObjectUtil;
import top.kdla.framework.dto.ErrorCode;
import top.kdla.framework.exception.BizException;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author kanglele
 * @version $Id: VertxHttpUtil, v 0.1 2023/5/17 18:36 kanglele Exp $
 */
@Component
public class VertxHttpClient {

    @Resource
    private WebClient webClient;

    private static final String CONTENT_TYPE_JSON = "application/json";

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
    public <T> CompletableFuture<T> getJson(String url, MultiMap headers, Class<T> res) {
        CompletableFuture<T> future = new CompletableFuture<>();
        webClient.getAbs(url)
                .putHeaders(headers)
                .putHeader("content-type", CONTENT_TYPE_JSON)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        future.complete(response.bodyAsJsonObject().mapTo(res));
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        return future;
    }

    public <T> CompletableFuture<T> postJson(String url, MultiMap headers, Object req, Class<T> res) {
        CompletableFuture<T> future = new CompletableFuture<>();
        webClient.postAbs(url)
                .putHeaders(headers)
                .putHeader("content-type", CONTENT_TYPE_JSON)
                .sendBuffer(Buffer.buffer(JSON.toJSONString(req)), ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        future.complete(response.bodyAsJsonObject().mapTo(res));
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        return future;
    }


    public <T> CompletableFuture<T> sendRequest(HttpMethod method, String url, Map headers, Object req, Class<T> res) {
        CompletableFuture<T> future = new CompletableFuture<>();
        HttpRequest<Buffer> request = createRequest(method, url, headers, req);
        request.send(ar -> {
            if (ar.succeeded()) {
                try {
                    HttpResponse<Buffer> response = ar.result();
                    future.complete(response.bodyAsJson(res));//默认json返回
                } catch (Exception e) {
                    throw new BizException(ErrorCode.FAIL.getCode(), "调用外部接口异常", e.getCause());
                }
            } else {
                throw new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败");
            }
        });
        return future;
    }

    /**
     *  Get请求的参数，请直接在url后拼接，不走addQueryParam
     * @param method
     * @param url
     * @param headers
     * @param req
     * @return
     */
    private HttpRequest<Buffer> createRequest(HttpMethod method, String url, Map headers, Object req) {
        Map headers2 = new HashMap();
        headers.forEach((k,v) -> {
            headers2.put(String.valueOf(k).toLowerCase(),v);
        });

        HttpRequest<Buffer> request = null;
        if (HttpMethod.GET.equals(method)) {
            request = webClient.getAbs(url);
            if (headers2.get(HttpHeaderNames.CONTENT_TYPE.toString()).equals(HttpHeaderValues.APPLICATION_JSON.toString())) {
                if (req instanceof String) {
                    request.sendBuffer(Buffer.buffer((String) req));
                } else {
                    request.sendJson(req);
                }
            }
        } else if (HttpMethod.POST.equals(method)) {
            request = webClient.postAbs(url);
            if (headers2.get(HttpHeaders.CONTENT_TYPE.toString()).equals(HttpHeaderValues.APPLICATION_JSON.toString())) {
                if (req instanceof String) {
                    request.sendBuffer(Buffer.buffer((String) req));
                } else {
                    request.sendJson(req);
                }
            } else if (headers2.get(HttpHeaders.CONTENT_TYPE.toString()).equals(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
                request.sendForm((MultiMap) req);
            } else if (headers2.get(HttpHeaders.CONTENT_TYPE.toString()).equals(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) {
                request.sendMultipartForm((MultipartForm) req);
            } else {
                byte[] data = ObjectUtil.ObjectToByte(req);
                request.sendBuffer(Buffer.buffer(data));
            }
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        MultiMap headerMap = HeadersMultiMap.httpHeaders();
        headerMap.setAll(headers);
        request.putHeaders(headerMap);
        return request;
    }

}
