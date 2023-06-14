package top.kdla.framework.supplement.http;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BaseHttpClient {

    @Autowired
    private RestTemplate restTemplate;

    private HttpHeaders headers;

    protected BaseHttpClient() {
        headers = new HttpHeaders();
    }

    public void setHeaders(Map<String, String> headerMap) {
        this.headers.setAll(headerMap);
    }

    public HttpHeaders getHeaders() {
        return this.headers;
    }

    public HttpEntity<String> get(String url) {
        log.info("BaseHttpClient.get:URL:{}" ,url);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, String.class);
    }

    public ResponseEntity<String> get(String url, Map<String, Object> params) {
        log.info("BaseHttpClient.get:URL:" + url + ";params:" + params.toString());
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        HttpEntity<?> entity = new HttpEntity<>(headers);

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        ResponseEntity<String> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is4xxClientError()) {
            return response;
        }

        log.error("Get error: {}", response.getBody());
        throw new RestClientException(response.getBody());
    }


    public HttpEntity<String> put(String url, Map<String, Object> body) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, String.class);
    }

    public HttpEntity<String> post(String url, Map body) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, String.class);
    }


    public HttpEntity<String> post(String url, List body) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST, entity, String.class);
    }

}
