package top.kdla.framework.log.webfilter;

import org.junit.jupiter.api.Test;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：构造器读取原始请求流抛 IOException 时曾只打日志、body 保持 null，
 * 后续 getInputStream() 调 body.getBytes() 直接 NPE；且 new String/getBytes 未指定字符集。
 * 修复后：catch 中 body 置空串兜底，getInputStream() 返回空流不 NPE，编解码显式 UTF-8。
 */
class KdlaHttpServletRequestWrapperBodyFallbackTest {

    @Test
    void constructor_ioException_bodyFallsBackToEmpty_andGetInputStreamReturnsEmptyStream() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getInputStream()).thenThrow(new IOException("connection reset"));

        KdlaHttpServletRequestWrapper wrapper = new KdlaHttpServletRequestWrapper(request);

        assertEquals("", wrapper.getBody(), "读取失败时body必须置空串兜底，不得为null");
        // 修复前：body为null，此处抛NPE
        ServletInputStream inputStream = assertDoesNotThrow(() -> wrapper.getInputStream(),
                "getInputStream()不得因body为null抛NPE");
        assertEquals(-1, inputStream.read(), "兜底后getInputStream()必须返回空流");
    }

    @Test
    void normalBody_utf8RoundTrip() throws IOException {
        final byte[] bodyBytes = "你好，body".getBytes(StandardCharsets.UTF_8);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getInputStream()).thenReturn(new ServletInputStream() {
            private final ByteArrayInputStream delegate = new ByteArrayInputStream(bodyBytes);

            @Override
            public boolean isFinished() {
                return delegate.available() == 0;
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
                return delegate.read();
            }
        });

        KdlaHttpServletRequestWrapper wrapper = new KdlaHttpServletRequestWrapper(request);

        // 显式UTF-8编解码，中文body不得乱码
        assertEquals("你好，body", wrapper.getBody(), "body必须按UTF-8解码");
        byte[] readBack = new byte[bodyBytes.length];
        ServletInputStream inputStream = wrapper.getInputStream();
        int readCount = 0;
        while (readCount < readBack.length) {
            int b = inputStream.read();
            if (b == -1) {
                break;
            }
            readBack[readCount++] = (byte) b;
        }
        assertEquals(bodyBytes.length, readCount, "getInputStream()必须按UTF-8回读完整body");
        assertEquals("你好，body", new String(readBack, StandardCharsets.UTF_8));
    }
}
