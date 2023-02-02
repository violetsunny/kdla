/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.utils;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import top.kdla.framework.common.enums.CharsetEnum;
import top.kdla.framework.common.enums.ContentTypeEnum;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author kanglele
 * @version $Id: HttpUtil, v 0.1 2023/2/2 14:34 kanglele Exp $
 */
public class HttpUtil {
    public HttpUtil() {
    }

    public static String doRequest(String requestUrl, String parameterData, RequestMethod requestMethod, CharsetEnum charset, ContentTypeEnum contentType, int timeOut) throws Exception {
        OutputStream outputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        HttpURLConnection connection = null;
        StringBuffer resultBuffer = new StringBuffer();

        try {
            if (StringUtils.isEmpty(parameterData)) {
                parameterData = "";
            }

            URL httpUrl = new URL(requestUrl);
            connection = (HttpURLConnection)httpUrl.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod(requestMethod.toString());
            connection.setRequestProperty("Content-Type", contentType.getValue() + ";" + charset.getName());
            if (!"get".equals(requestMethod.toString().toLowerCase())) {
                connection.setRequestProperty("Content-Length", String.valueOf(parameterData.length()));
            }

            connection.setConnectTimeout(timeOut);
            connection.setReadTimeout(timeOut);
            outputStream = connection.getOutputStream();
            outputStreamWriter = new OutputStreamWriter(outputStream, charset.getName());
            if (!"get".equals(requestMethod.toString().toLowerCase())) {
                outputStreamWriter.write(parameterData);
            }

            outputStreamWriter.flush();
            if (connection.getResponseCode() >= 300) {
                throw new Exception("HTTP Request is not success, Response code is " + connection.getResponseCode());
            }

            inputStream = connection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream, charset.getName());
            reader = new BufferedReader(inputStreamReader);
            String tempLine = null;

            while((tempLine = reader.readLine()) != null) {
                resultBuffer.append(tempLine);
            }
        } catch (Exception var18) {
            throw var18;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (reader != null) {
                reader.close();
            }

            if (inputStreamReader != null) {
                inputStreamReader.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }

        }

        return resultBuffer.toString();
    }

    public static String doPost(String requestUrl, String requestData, int timeout) throws Exception {
        return doRequest(requestUrl, requestData, RequestMethod.POST, CharsetEnum.UTF8, ContentTypeEnum.TEXT_XML, timeout);
    }

    public static String doPost(String requestUrl, String requestData, CharsetEnum charset, int timeout) throws Exception {
        return doRequest(requestUrl, requestData, RequestMethod.POST, charset, ContentTypeEnum.APPLICATION_JSON, timeout);
    }

    public static String doPost(String requestUrl, String requestData, ContentTypeEnum contentType, int timeout) throws Exception {
        return doRequest(requestUrl, requestData, RequestMethod.POST, CharsetEnum.UTF8, contentType, timeout);
    }

    public static String doGet(String requestUrl, CharsetEnum charset, int timeout) throws Exception {
        return doRequest(requestUrl, (String)null, RequestMethod.GET, charset, ContentTypeEnum.APPLICATION_JSON, timeout);
    }

    public static String doGet(String requestUrl, ContentTypeEnum contentType, int timeout) throws Exception {
        return doRequest(requestUrl, (String)null, RequestMethod.GET, CharsetEnum.UTF8, contentType, timeout);
    }

    public static String doGet(String requestUrl, int timeout) throws Exception {
        return doRequest(requestUrl, (String)null, RequestMethod.GET, CharsetEnum.UTF8, ContentTypeEnum.TEXT_XML, timeout);
    }

    public static String getData(String requestUrl, int timeout) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        Throwable var3 = null;

        String var28;
        try {
            String rstString = "";
            HttpGet httpGet = new HttpGet(requestUrl);
            httpGet.addHeader("Content-Type", "text/html; charset=UTF-8");
            RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(timeout).setConnectTimeout(timeout).setSocketTimeout(timeout).build();
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = null;

            try {
                response = httpClient.execute(httpGet);
                if (response != null && response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null) {
                    rstString = inputStreamToStr(response.getEntity().getContent(), StandardCharsets.UTF_8.name());
                }
            } catch (Exception var24) {
                Exception e = var24;
                throw var24;
            } finally {
                if (response != null) {
                    response.close();
                }

                httpGet.releaseConnection();
            }

            var28 = rstString;
        } catch (Throwable var26) {
            var3 = var26;
            throw var26;
        } finally {
            if (httpClient != null) {
                if (var3 != null) {
                    try {
                        httpClient.close();
                    } catch (Throwable var23) {
                        var3.addSuppressed(var23);
                    }
                } else {
                    httpClient.close();
                }
            }

        }

        return var28;
    }

    public static String inputStreamToStr(InputStream inputStream, String charsetName) throws Exception {
        if (inputStream != null) {
            try {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];

                int length;
                while((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                return result.toString(charsetName);
            } catch (Exception var5) {
                throw var5;
            }
        } else {
            return null;
        }
    }
}
