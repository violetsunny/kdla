/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.MediaType;
import top.kdla.framework.common.utils.ObjectUtil;
import top.kdla.framework.common.utils.RegexUtil;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Locale;
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

    private final WebClient webClientHttp;
    private final WebClient webClientHttps;

    public VertxHttpClient(WebClient webClientHttp, WebClient webClientHttps) {
        this.webClientHttp = webClientHttp;
        this.webClientHttps = webClientHttps;
    }

    private WebClient getWebClient(String url) {
        return url.startsWith("https") ? this.webClientHttps : this.webClientHttp;
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
        HttpRequest<Buffer> request = getWebClient(url).getAbs(url).putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE);
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
        HttpRequest<Buffer> request = getWebClient(url).postAbs(url).putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE);
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
        Future<HttpResponse<Buffer>> responseFuture = this.createRequest(method, url, headers, req);
        responseFuture.onComplete(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                try {
                    if (res.equals(String.class)) {
                        String result = response.bodyAsString();
                        future.complete((T) result);
                        if (log.isInfoEnabled()) {
                            log.info("VertxHttpClient-send url:{} req:{} result:{}", url, JSON.toJSONString(req), result);
                        }
                    } else {
                        T result = response.bodyAsJson(res);//默认json返回
                        future.complete(result);
                        if (log.isInfoEnabled()) {
                            log.info("VertxHttpClient-send url:{} req:{} result:{}", url, JSON.toJSONString(req), JSON.toJSONString(result));
                        }
                    }
                } catch (Exception e) {
                    log.warn("VertxHttpClient-send url:{} req:{} \n result:{}", url, JSON.toJSONString(req), response.bodyAsString());
                    future.completeExceptionally(e.getCause());
                    throw new BizException(ErrorCode.FAIL.getCode(), e.getCause(), "调用外部接口异常：%s", response.bodyAsString());
                }
            } else {
                future.completeExceptionally(ar.cause());
                throw new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败");
            }
        });
        return future;
    }

    public <T> CompletableFuture<T> sendRequest(String method, String url, Map<String, String> headers, Object req, Class<T> res) {
        return this.sendRequest(HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), url, headers, req, res);
    }

    public CompletableFuture<HttpResponse<Buffer>> sendRequest(String method, String url, Map<String, String> headers, Object req) {
        CompletableFuture<HttpResponse<Buffer>> future = new CompletableFuture<>();
        Future<HttpResponse<Buffer>> responseFuture = this.createRequest(HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), url, headers, req);
        responseFuture.onComplete(ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result());
            } else {
                future.completeExceptionally(ar.cause());
                throw new BizException(ErrorCode.FAIL.getCode(), ar.cause(), "调用外部接口失败");
            }
        });
        return future;
    }

    /**
     * Get请求的参数，请直接在url后拼接，不走addQueryParam
     *
     * @param method
     * @param url
     * @param header
     * @param req
     * @return
     */
    private Future<HttpResponse<Buffer>> createRequest(HttpMethod method, String url, Map<String, String> header, Object req) {
        if (RegexUtil.validateChinese(url)) {
            throw new BizException(ErrorCode.FAIL.getCode(), "有中文字符，需要重新编码再请求：%s", url);
        }
        Map<String, String> headers;
        if (header != null) {
            //指定为小写
            headers = header.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
        } else {
            headers = new HashMap<>();
        }
        headers.putIfAbsent(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE);

        HttpRequest<Buffer> request = null;
        //HTTP method
        if (HttpMethod.GET.equals(method)) {
            request = getWebClient(url).getAbs(url);
        } else if (HttpMethod.POST.equals(method)) {
            request = getWebClient(url).postAbs(url);
        } else if (HttpMethod.PUT.equals(method)) {
            request = getWebClient(url).putAbs(url);
        } else if (HttpMethod.PATCH.equals(method)) {
            request = getWebClient(url).patchAbs(url);
        } else if (HttpMethod.DELETE.equals(method)) {
            request = getWebClient(url).deleteAbs(url);
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
                MultiMap form = transformMultiMap(JSONObject.parseObject(JSON.toJSONString(req)));
                responseFuture = request.sendForm(form);
            } else if (contentType.equalsIgnoreCase(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) {
                responseFuture = request.sendMultipartForm((MultipartForm) req);//文件上传下载 req必须是MultipartForm对象
            } else {
                byte[] data = ObjectUtil.ObjectToByte(req);
                responseFuture = request.sendBuffer(Buffer.buffer(data));
            }
        } else {
            responseFuture = request.send();
        }

        return responseFuture;
    }

    private MultiMap transformMultiMap(JSONObject jOb) {
        // 创建一个新的MultiMap
        MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();

        convertJSONObjectToMultiMap(jOb, multiMap, null);

        return multiMap;
    }

    private static void convertJSONObjectToMultiMap(JSONObject jOb, MultiMap multiMap, String parentKey) {
        for (String key : jOb.keySet()) {
            Object value = jOb.get(key);
            String newKey = (parentKey != null) ? parentKey + "." + key : key;
            if (value instanceof JSONObject) {
                // 如果值是一个嵌套的JSONObject，递归地处理它
                convertJSONObjectToMultiMap((JSONObject) value, multiMap, newKey);
            } else if (value instanceof JSONArray) {
                // 如果值是一个JSONArray，将其转换为一个字符串列表并添加到MultiMap中
                JSONArray array = (JSONArray) value;
                convertJSONArrayToMultiMap(array, multiMap, newKey);
            } else {
                // 否则，直接将值添加到MultiMap中
                multiMap.add(newKey, String.valueOf(value));
            }
        }
    }

    private static void convertJSONArrayToMultiMap(JSONArray jArray, MultiMap multiMap, String newKey) {
        for (Object object : jArray) {
            if (object instanceof JSONObject) {
                JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(object));
                convertJSONObjectToMultiMap(jsonObject, multiMap, newKey);
            } else {
                multiMap.add(newKey, String.valueOf(object));
            }
        }
    }


    @PreDestroy
    @ConditionalOnClass({Vertx.class, WebClient.class})
    public void close() {
        if (this.webClientHttp != null) {
            this.webClientHttp.close();
        }
        if (this.webClientHttps != null) {
            this.webClientHttps.close();
        }
    }

}
