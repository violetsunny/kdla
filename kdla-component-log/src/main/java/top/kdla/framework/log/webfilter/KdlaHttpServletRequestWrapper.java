package top.kdla.framework.log.webfilter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * http请求wrapper
 *
 * @author kll
 * @date 2021/8/2
 */
@Slf4j
public class KdlaHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 保存request body的数据
     */
    private String body;

    /**
     * 解析request的inputStream(即body)数据，转成字符串
     *
     * @param request HttpServletRequest
     */
    public KdlaHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        try {
            this.setBody(new String(IOUtils.toByteArray(request.getInputStream())));
        } catch (IOException ex) {
            log.error("IOException:{}", ExceptionUtils.getStackTrace(ex));
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
        return servletInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    public String getBody() {
        return this.body;
    }

    /**
     * 赋值给body字段
     *
     * @param body
     */
    public void setBody(String body) {
        this.body = body;
    }

}
