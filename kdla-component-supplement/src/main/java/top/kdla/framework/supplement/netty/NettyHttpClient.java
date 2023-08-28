/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.ttl.TransmittableThreadLocal;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultPromise;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kdla.framework.common.help.ThreadPoolHelp;
import top.kdla.framework.dto.ErrorCode;
import top.kdla.framework.exception.BizException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author kanglele
 * @version $Id: NettyHttpUtil, v 0.1 2023/5/17 18:22 kanglele Exp $
 */
@Slf4j
public class NettyHttpClient {

    @Setter
    private Boolean keepAlive = false;
    @Setter
    private Boolean ssl;
    @Setter
    private Map<String, String> headers;

    private final ThreadPoolHelp threadPoolHelp = new ThreadPoolHelp();

    @Getter
    private final TransmittableThreadLocal<String> response = new TransmittableThreadLocal<>();

    public String connect(String url, Object msg, HttpMethod method) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup(1, threadPoolHelp.getDefaultExecutorService());
        AtomicBoolean close = new AtomicBoolean(false);
        OutputResultHandler2 outputHandler = new OutputResultHandler2();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .remoteAddress(getInetAddress(url))
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .option(ChannelOption.SO_KEEPALIVE, this.keepAlive)
                    .handler(new HttpClientInitializer(ssl, outputHandler));

            // Start the client.
            ChannelFuture f = b.connect();
            DefaultPromise<String> respPromise = new DefaultPromise<>(f.channel().eventLoop());
            outputHandler.setResp(respPromise);
            HttpRequest request = getRequestMethod(url, method, msg);
            // 发送http请求
            f.channel().writeAndFlush(request);
            //f.channel().closeFuture().sync();//不建议使用阻塞方式
            f.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    workerGroup.shutdownGracefully();
                    close.set(true);
                    log.info(future.channel().toString() + "链路关闭");
                }
            });

            //response
            return respPromise.get();
        } finally {
            if (!close.get()) {
                workerGroup.shutdownGracefully();
            }
        }

    }

    private class HttpClientInitializer extends ChannelInitializer<SocketChannel> {
        private final boolean ssl;

        private final OutputResultHandler2 outputHandler;

        public HttpClientInitializer(boolean ssl, OutputResultHandler2 outputHandler) {
            this.ssl = ssl;
            this.outputHandler = outputHandler;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            if (this.ssl) {
                //配置Https通信
                SslContext context = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                ch.pipeline().addLast(context.newHandler(ch.alloc()));
                //SSLEngine engine = SslContextFactory.getClientContext().createSSLEngine();
                //engine.setUseClientMode(true);
                //ch.pipeline().addLast("ssl", new SslHandler(engine));
            }
            ch.pipeline().addLast(new HttpClientCodec());
            //HttpObjectAggregator会将多个HttpResponse和HttpContents对象再拼装成一个单一的FullHttpRequest或是FullHttpResponse
            ch.pipeline().addLast("aggre", new HttpObjectAggregator(10 * 1024 * 1024));
            //解压
            ch.pipeline().addLast("decompressor", new HttpContentDecompressor());
            // 获取返回
            ch.pipeline().addLast(outputHandler);

        }
    }

    private SocketAddress getInetAddress(String url) throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        if (this.ssl == null) {
            this.ssl = "https".equalsIgnoreCase(scheme);
        }

        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        InetSocketAddress inetAddress = null;
        InetAddress address = InetAddress.getByName(host);
        if (!host.equalsIgnoreCase(address.getHostAddress())) {
            //域名连接,https默认端口是443，http默认端口是80
            inetAddress = new InetSocketAddress(address, this.ssl ? 443 : 80);
        } else {
            //ip+端口连接
            int port = uri.getPort();
            inetAddress = InetSocketAddress.createUnresolved(host, port);
        }
        return inetAddress;
    }

    private HttpRequest getRequestMethod(String url, HttpMethod method, Object msg) throws Exception {
        URI uri2 = new URI(url);
        String host = uri2.getHost();
        String path = uri2.getRawPath();
//        String getPath = uri2.toString();//get
//        URL netUrl = new URL(url);
//        URI uri = new URI(netUrl.getPath());
//        String path = uri.toASCIIString();//post


        DefaultFullHttpRequest request = null;
        if (method.equals(HttpMethod.POST)) {
            // 构建http请求
            if (this.headers.get(HttpHeaderNames.CONTENT_TYPE.toString()) != null &&
                    HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString().equalsIgnoreCase(this.headers.get(HttpHeaderNames.CONTENT_TYPE.toString()))) {

                List<BasicNameValuePair> formData = new ArrayList<>();
                Set<Map.Entry<String, Object>> entrySet = JSONObject.parseObject(JSON.toJSONString(msg)).entrySet();
                for (Map.Entry<String, Object> e : entrySet) {
                    String key = e.getKey();
                    Object value = e.getValue();
                    formData.add(new BasicNameValuePair(key, String.valueOf(value)));
                }
                HttpEntity httpEntity = new UrlEncodedFormEntity(formData, StandardCharsets.UTF_8);
                ByteBuf byteBuf = Unpooled.wrappedBuffer(EntityUtils.toByteArray(httpEntity));
                request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path, byteBuf);
                request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
            } else {
                ByteBuf byteBuf = Unpooled.wrappedBuffer(JSON.toJSONBytes(msg));
                request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path, byteBuf);
                request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
            }

        } else if (method.equals(HttpMethod.GET)) {
            // 构建http请求
            QueryStringEncoder encoder = new QueryStringEncoder(path);
            // 添加原始查询参数
            if (uri2.getRawQuery() != null) {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri2.getRawQuery(), false);
                for (Map.Entry<String, List<String>> entry : queryStringDecoder.parameters().entrySet()) {
                    for (String value : entry.getValue()) {
                        encoder.addParam(entry.getKey(), value);
                    }
                }
            }
            if (msg != null) {
                Set<Map.Entry<String, Object>> entrySet = JSONObject.parseObject(JSON.toJSONString(msg)).entrySet();
                for (Map.Entry<String, Object> e : entrySet) {
                    String key = e.getKey();
                    Object value = e.getValue();
                    encoder.addParam(key, String.valueOf(value));
                }
            }
//            String fullPath = uri2.getScheme() + "://" + uri2.getHost() + encoder.toString();
            request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, encoder.toString());

        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        request.headers().set(HttpHeaderNames.HOST, host);
        if (this.keepAlive) {
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        //其他头部信息
        if (this.headers != null && !this.headers.isEmpty()) {
            for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                request.headers().set(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    public class OutputResultHandler extends ChannelInboundHandlerAdapter {

        private final Logger log = LoggerFactory.getLogger(OutputResultHandler.class);

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse res = (HttpResponse) msg;
                log.info(res.toString());
                response.set(res.toString());
            }
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                ByteBuf buf = content.content();
                log.info(buf.toString(CharsetUtil.UTF_8));
                response.set(buf.toString(CharsetUtil.UTF_8));
                //buf.release();
            }

            ReferenceCountUtil.safeRelease(msg);
        }
    }

    public class OutputResultHandler2 extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final Logger log = LoggerFactory.getLogger(OutputResultHandler2.class);

        @Setter
        private DefaultPromise<String> resp;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            if (!msg.headers().isEmpty()) {
                for (String name : msg.headers().names()) {
                    for (String value : msg.headers().getAll(name)) {
                        log.info("HEADER: " + name + " = " + value);
                    }
                }
            }

            if (msg.decoderResult().isFailure()) {
                resp.setFailure(new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败", msg.decoderResult().cause()));
                //throw new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败");
            } else {
                ByteBuf buf = msg.content();
                log.info("content:{}", buf.toString(CharsetUtil.UTF_8));
                response.set(buf.toString(CharsetUtil.UTF_8));
                resp.setSuccess(buf.toString(CharsetUtil.UTF_8));
            }

            //ReferenceCountUtil.safeRelease(msg);
        }

    }

}
