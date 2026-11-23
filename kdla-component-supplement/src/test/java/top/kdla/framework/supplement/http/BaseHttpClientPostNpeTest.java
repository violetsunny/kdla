package top.kdla.framework.supplement.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 清单#主题4 回归测试（移植自1.x）：BaseHttpClient.post在headerMap为null或缺content-type键时不抛NPE。
 *
 * <p>旧实现直接headerMap.get(CONTENT_TYPE.toLowerCase()).equalsIgnoreCase(...)：
 * headerMap为null时get触发NPE；缺content-type键时get返回null再调equalsIgnoreCase触发NPE。
 *
 * <p>无修复则失败：旧代码在两种入参下均抛NPE，下方assertDoesNotThrow不通过。
 */
@ExtendWith(MockitoExtension.class)
class BaseHttpClientPostNpeTest {

    @Mock
    private RestTemplate restTemplate;

    private BaseHttpClient client;

    private final Map<String, Object> body = new HashMap<>();

    @BeforeEach
    void setUp() {
        client = new BaseHttpClient(restTemplate);
        body.put("k", "v");
    }

    private void stubExchange() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));
    }

    @Test
    void post_nullHeaderMap_noNpe() {
        stubExchange();
        ResponseEntity<String> resp =
                assertDoesNotThrow(() -> client.post("http://localhost/test", null, body));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void post_headerMapWithoutContentType_noNpe() {
        stubExchange();
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer x");
        ResponseEntity<String> resp =
                assertDoesNotThrow(() -> client.post("http://localhost/test", headers, body));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}