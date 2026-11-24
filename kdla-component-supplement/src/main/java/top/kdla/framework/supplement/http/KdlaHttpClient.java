/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.supplement.http;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.util.Locale;
import java.util.Map;

/**
 * @author kanglele
 * @version $Id: KdlaHttpClient, v 0.1 2024/3/21 17:52 kanglele Exp $
 */
public class KdlaHttpClient extends HttpUtil {

    public String send(String method, String url, Map<String, String> header, Object req) {
        return buildRequest(Method.valueOf(method.toUpperCase(Locale.ROOT)), url)
                .headerMap(header, true)
                .form(JSONUtil.toBean(JSONUtil.toJsonStr(req), new TypeReference<Map<String, Object>>() {}, false))
                .execute()
                .body();
    }

    //hutool默认trust-all工厂+恒真域名校验，显式恢复JDK默认信任链与域名校验防MITM
    static HttpRequest buildRequest(Method method, String url) {
        return createRequest(method, url)
                .setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault())
                .setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
    }

}
