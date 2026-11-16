package top.kdla.framework.log.webfilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：handleExcludeURL注释声明"GET DELETE 不过滤"，但代码只判了DELETE，
 * GET请求携带application/json头时仍会被包装读取body（无谓的流拷贝开销）。
 * 修复后：GET与DELETE均直接放行原始request。
 */
class KdlaBodyReaderRequestFilterGetMethodTest {

    private KdlaBodyReaderRequestFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new KdlaBodyReaderRequestFilter();
        // @Value字段在单测中不注入，反射赋默认排除列表（T分叉：List<String> excludes，R为String exclude）
        Field excludes = KdlaBodyReaderRequestFilter.class.getDeclaredField("excludes");
        excludes.setAccessible(true);
        excludes.set(filter, Arrays.asList("/config/**", "/ping"));
    }

    private ServletInputStream servletInputStream(String body) {
        ByteArrayInputStream bais = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bais.read();
            }
        };
    }

    @Test
    void getRequest_withJsonContentType_notWrapped() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getServletPath()).thenReturn("/api/query");

        filter.doFilter(request, response, chain);

        // 修复前：GET+JSON会被包装成KdlaHttpServletRequestWrapper传给下游
        verify(chain).doFilter(eq(request), eq(response));
    }

    @Test
    void deleteRequest_withJsonContentType_notWrapped() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getServletPath()).thenReturn("/api/resource");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(eq(request), eq(response));
    }

    @Test
    void postJsonRequest_stillWrapped() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getServletPath()).thenReturn("/api/create");
        when(request.getInputStream()).thenReturn(servletInputStream("{\"a\":1}"));

        filter.doFilter(request, response, chain);

        ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
        verify(chain).doFilter(captor.capture(), eq(response));
        assertTrue(captor.getValue() instanceof KdlaHttpServletRequestWrapper,
                "POST+JSON仍必须包装为可重复读wrapper");
    }
}
