package top.kdla.framework.supplement.http;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BaseHttpClient {

    private final RestTemplate restTemplate;

    public BaseHttpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders setHeader(Map<String, String> headerMap) {
        if (headerMap != null) {
            //指定为小写
            headerMap = headerMap.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
        } else {
            headerMap = new HashMap<>();
        }
        headerMap.putIfAbsent(HttpHeaders.CONTENT_TYPE.toLowerCase(), MediaType.APPLICATION_JSON_VALUE);
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(headerMap);
        return headers;
    }

    public ResponseEntity<String> get(String url, Map<String, String> headerMap, Map<String, Object> params) {
        if (log.isInfoEnabled()) {
            log.info("BaseHttpClient.get:URL:{};params:{}", url, JSON.toJSONString(params));
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }

        HttpEntity<?> entity = new HttpEntity<>(setHeader(headerMap));
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is4xxClientError()) {
            return response;
        }

        log.error("Get error: {}", response.getBody());
        throw new RestClientException(url + " 请求异常");
    }


    public ResponseEntity<String> put(String url, Map<String, String> headerMap, Map<String, Object> body) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        HttpEntity<?> entity = new HttpEntity<>(body, setHeader(headerMap));
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is4xxClientError()) {
            return response;
        }

        log.error("put error: {}", response.getBody());
        throw new RestClientException(url + " 请求异常");
    }

    public ResponseEntity<String> post(String url, Map<String, String> headerMap, Object body) {
        if (log.isInfoEnabled()) {
            log.info("BaseHttpClient.post URL:{} body:{}", url, JSON.toJSONString(body));
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if(headerMap.get(HttpHeaders.CONTENT_TYPE.toLowerCase()).equalsIgnoreCase(MediaType.APPLICATION_FORM_URLENCODED_VALUE)){
            MultiValueMap<String, Object> req = new LinkedMultiValueMap<>();
            req.setAll(JSONObject.parseObject(JSON.toJSONString(body)));
            body = req;
        }
        HttpEntity<?> entity = new HttpEntity<>(body, setHeader(headerMap));
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is4xxClientError()) {
            return response;
        }

        log.error("post error: {}", response.getBody());
        throw new RestClientException(url + " 请求异常");
    }

}
