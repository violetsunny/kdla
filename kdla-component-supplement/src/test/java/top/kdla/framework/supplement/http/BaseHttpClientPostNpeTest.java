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
 * 清单#主题4 回归测试：BaseHttpClient.post 在 headerMap 为 null 或缺 content-type 键时不抛 NPE。
 *
 * <p>旧实现直接 headerMap.get(CONTENT_TYPE.toLowerCase()).equalsIgnoreCase(...)：
 * headerMap 为 null 时 get 触发 NPE；缺 content-type 键时 get 返回 null 再调 equalsIgnoreCase 触发 NPE。
 *
 * <p>修复后：先做 null 安全取值，再用常量.equalsIgnoreCase(contentType) 判断。
 *
 * <p>无修复则失败：旧代码在两种入参下均抛 NPE，下方 assertDoesNotThrow 不通过。
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
