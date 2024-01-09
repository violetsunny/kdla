package top.kdla.framework.supplement.http;


import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
public class BaseHttpClient {

    private final RestTemplate restTemplate;

    @Getter
    private final HttpHeaders headers;

    public BaseHttpClient(RestTemplate restTemplate) {
        this.headers = new HttpHeaders();
        this.restTemplate = restTemplate;
    }

    public void setHeaders(Map<String, String> headerMap) {
        this.headers.setAll(headerMap);
    }


    public ResponseEntity<String> get(String url, Map<String, Object> params) {
        if (log.isInfoEnabled()) {
            log.info("BaseHttpClient.get:URL:{};params:{}", url, JSON.toJSONString(params));
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is4xxClientError()) {
            return response;
        }

        log.error("Get error: {}", response.getBody());
        throw new RestClientException(url + " 请求异常");
    }


    public ResponseEntity<String> put(String url, Map<String, Object> body) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is4xxClientError()) {
            return response;
        }

        log.error("put error: {}", response.getBody());
        throw new RestClientException(url + " 请求异常");
    }

    public ResponseEntity<String> post(String url, Object body) {
        if (log.isInfoEnabled()) {
            log.info("BaseHttpClient.post URL:{} body:{}", url, JSON.toJSONString(body));
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is4xxClientError()) {
            return response;
        }

        log.error("post error: {}", response.getBody());
        throw new RestClientException(url + " 请求异常");
    }

}
