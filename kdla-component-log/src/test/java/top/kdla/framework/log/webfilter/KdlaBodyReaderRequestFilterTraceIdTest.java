package top.kdla.framework.log.webfilter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * 回归：traceId生成与finally清理被删，Tomcat线程复用traceId串号；excludes空判定被删。
 * 修复后：过滤器生命周期内生成/透传traceId并在出口清理；excludes为空直接放行。
 */
class KdlaBodyReaderRequestFilterTraceIdTest {

    private KdlaBodyReaderRequestFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new KdlaBodyReaderRequestFilter();
        setExcludes(Arrays.asList("/config/**", "/ping"));
        MDC.remove(LOG_TRACE_ID);
    }

    @AfterEach
    void tearDown() {
        MDC.remove(LOG_TRACE_ID);
    }

    private void setExcludes(Object value) throws Exception {
        Field excludes = KdlaBodyReaderRequestFilter.class.getDeclaredField("excludes");
        excludes.setAccessible(true);
        excludes.set(filter, value);
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

    private HttpServletRequest postJsonRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn("application/json");
        when(request.getServletPath()).thenReturn("/api/create");
        when(request.getInputStream()).thenReturn(servletInputStream("{\"a\":1}"));
        return request;
    }

    private AtomicReference<String> captureTraceInChain(FilterChain chain) throws Exception {
        AtomicReference<String> traceInChain = new AtomicReference<>();
        doAnswer(invocation -> {
            traceInChain.set(MDC.get(LOG_TRACE_ID));
            return null;
        }).when(chain).doFilter(any(), any());
        return traceInChain;
    }

    @Test
    void doFilter_generatesTraceId_duringChain_andCleansAfter() throws Exception {
        HttpServletRequest request = postJsonRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<String> traceInChain = captureTraceInChain(chain);

        filter.doFilter(request, response, chain);

        // 修复前：过滤器不生成traceId，下游日志无trace
        assertNotNull(traceInChain.get(), "下游chain执行前必须生成traceId");
        assertNull(MDC.get(LOG_TRACE_ID), "出口必须清理traceId，防线程复用串号");
    }

    @Test
    void doFilter_upstreamHeaderTraceId_propagated() throws Exception {
        HttpServletRequest request = postJsonRequest();
        when(request.getHeader(LOG_TRACE_ID)).thenReturn("upstream-trace-1");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<String> traceInChain = captureTraceInChain(chain);

        filter.doFilter(request, response, chain);

        assertEquals("upstream-trace-1", traceInChain.get(), "请求头携带的上游traceId必须透传");
        assertNull(MDC.get(LOG_TRACE_ID), "出口必须清理traceId");
    }

    @Test
    void doFilter_crlfHeaderTraceId_rejectedAndSelfGenerated() throws Exception {
        // D-12 回归：含 CRLF 的外来 traceId 旧实现直接进 MDC 可伪造日志行，修复后必须自生成
        HttpServletRequest request = postJsonRequest();
        String malicious = "abc\r\n2026-11-24 INFO forged-log-line";
        when(request.getHeader(LOG_TRACE_ID)).thenReturn(malicious);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<String> traceInChain = captureTraceInChain(chain);

        filter.doFilter(request, response, chain);

        assertNotNull(traceInChain.get());
        assertFalse(traceInChain.get().contains("\r"), "非法traceId不得透传进MDC");
        assertFalse(traceInChain.get().contains("\n"), "非法traceId不得透传进MDC");
        assertNull(MDC.get(LOG_TRACE_ID), "出口必须清理traceId");
    }

    @Test
    void emptyExcludes_postJson_passesThroughUnwrapped() throws Exception {
        setExcludes(Collections.emptyList());
        HttpServletRequest request = postJsonRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // 修复前：excludes为空不短路，POST+JSON仍被包装
        verify(chain).doFilter(eq(request), eq(response));
    }
}