/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultPromise;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kdla.framework.common.help.ThreadPoolHelp;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.http.ssl.SslContextFactory;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author kanglele
 * @version $Id: NettyHttpUtil, v 0.1 2023/5/17 18:22 kanglele Exp $
 */
@Slf4j
public class NettyHttpClient {

    //该类非Spring Bean（由使用方new创建），@Value永不注入致connectTimeout恒0；改为显式默认值
    private int connectTimeout = 3000;

    private boolean keepAlive = false;
    private boolean ssl = false;
    private File client;
    private File clienttruststore;
    private String keyStorePassword;

    private final EventLoopGroup workerGroup;
    private final Bootstrap bootstrap;

    public NettyHttpClient(String url, boolean keepAlive) throws Exception {
        this.keepAlive = keepAlive;

        workerGroup = new NioEventLoopGroup(1, new ThreadPoolHelp().getDefaultExecutorService());
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .remoteAddress(getInetAddress(url))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .option(ChannelOption.SO_KEEPALIVE, keepAlive)
                .handler(new HttpClientInitializer(this.ssl, this.client, this.clienttruststore, this.keyStorePassword));
    }

    public void close() {
        workerGroup.shutdownGracefully();
    }

    public NettyHttpClient setSsl(File client, File clienttruststore, String keyStorePassword) {
        this.ssl = true;
        this.client = client;
        this.clienttruststore = clienttruststore;
        this.keyStorePassword = keyStorePassword;
        //构造器按值捕获参数建initializer，事后setSsl须重建handler否则mTLS配置不生效
        bootstrap.handler(new HttpClientInitializer(this.ssl, this.client, this.clienttruststore, this.keyStorePassword));
        return this;
    }

    //响应等待上限毫秒；无超时的get()在对端不回包且不断连时永久挂起调用线程
    private long requestTimeout = 30000;

    public NettyHttpClient setRequestTimeout(long requestTimeoutMillis) {
        this.requestTimeout = requestTimeoutMillis;
        return this;
    }

    public String sendRequestPromise(String url, Object reqMsg, String method, Map<String, String> headers) throws Exception {
        DefaultPromise<String> respPromise = new DefaultPromise<>(workerGroup.next());
        try {
            // Start the client.
            ChannelFuture f = bootstrap.connect();
            OutputResultHandlerPromise outputResultHandler = new OutputResultHandlerPromise();
            outputResultHandler.setResponse(respPromise);
            // 获取返回
            f.channel().pipeline().addLast(outputResultHandler);
            // 发送http请求
            HttpRequest request = getRequestMethod(url, reqMsg, HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), headers);
            f.channel().writeAndFlush(request);
            //连接在响应读完、对端关闭或异常时才关闭；finally立即close会截断响应
            f.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    workerGroup.shutdownGracefully();
                    if (log.isInfoEnabled()) {
                        log.info("{} 链路关闭", future.channel().toString());
                    }
                }
            });

        } catch (Exception e) {
            respPromise.setFailure(e);
            workerGroup.shutdownGracefully();
        }

        //response
        try {
            return respPromise.get(requestTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    public String sendRequestCompletable(String url, Object reqMsg, String method, Map<String, String> headers) throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            // Start the client.
            ChannelFuture f = bootstrap.connect();
            OutputResultHandlerCompletable outputResultHandler = new OutputResultHandlerCompletable();
            outputResultHandler.setResponse(future);
            // 获取返回
            f.channel().pipeline().addLast(outputResultHandler);
            // 发送http请求
            HttpRequest request = getRequestMethod(url, reqMsg, HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), headers);
            f.channel().writeAndFlush(request);
            //连接在响应读完、对端关闭或异常时才关闭；finally立即close会截断响应
            f.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    workerGroup.shutdownGracefully();
                    if (log.isInfoEnabled()) {
                        log.info("{} 链路关闭", future.channel().toString());
                    }
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(e);
            workerGroup.shutdownGracefully();
        }

        //response
        try {
            return future.get(requestTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    private HttpResponseCallback<String> responseCallback;

    public NettyHttpClient chooseAsync(HttpResponseCallback<String> responseCallback) {
        this.responseCallback = responseCallback;
        return this;
    }

    public void sendRequestAsync(String url, Object reqMsg, String method, Map<String, String> headers) throws Exception {
        try {
            // Start the client.
            ChannelFuture f = bootstrap.connect();
            // 获取返回
            f.channel().pipeline().addLast(new OutputResultHandlerAsync(this.responseCallback));
            // 发送http请求
            HttpRequest request = getRequestMethod(url, reqMsg, HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)), headers);
            f.channel().writeAndFlush(request);
            //连接在响应读完、对端关闭或异常时才关闭；finally立即close会截断响应
            f.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    workerGroup.shutdownGracefully();
                    if (log.isInfoEnabled()) {
                        log.info("{} 链路关闭", future.channel().toString());
                    }
                }
            });

        } catch (Exception e) {
            this.responseCallback.onError(e);
            workerGroup.shutdownGracefully();
        }

    }

    private static class HttpClientInitializer extends ChannelInitializer<SocketChannel> {
        private final boolean ssl;
        private final File client;
        private final File clienttruststore;
        private final String keyStorePassword;

        public HttpClientInitializer(boolean ssl, File client, File clienttruststore, String keyStorePassword) {
            this.ssl = ssl;
            this.client = client;
            this.clienttruststore = clienttruststore;
            this.keyStorePassword = keyStorePassword;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            if (this.ssl) {
                //配置Https通信
                //开发测试模式
                //SslContext context = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                //SSLEngine sslEngine = context.newEngine(ch.alloc());
                //生产模式
                SSLEngine sslEngine;
                if (client != null && clienttruststore != null && keyStorePassword != null) {
                    sslEngine = SslContextFactory.getClientContext(client, clienttruststore, keyStorePassword).createSSLEngine();
                } else {
                    //https未提供自定义keystore时走JDK默认信任链；传null给SslContextFactory必NPE
                    sslEngine = javax.net.ssl.SSLContext.getDefault().createSSLEngine();
                }
                sslEngine.setUseClientMode(true);
                ch.pipeline().addFirst("ssl", new SslHandler(sslEngine));
            }
            ch.pipeline().addLast(new HttpClientCodec());
            //HttpObjectAggregator会将多个HttpResponse和HttpContents对象再拼装成一个单一的FullHttpRequest或是FullHttpResponse
            ch.pipeline().addLast("aggre", new HttpObjectAggregator(10 * 1024 * 1024));
            //解压
            ch.pipeline().addLast("decompressor", new HttpContentDecompressor());
        }
    }

    private SocketAddress getInetAddress(String url) throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        if (!this.ssl) {
            this.ssl = "https".equalsIgnoreCase(scheme);
        }

        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        InetSocketAddress inetAddress;
        //InetAddress.getByName(host).isReachable(30000);
        InetAddress address = InetAddress.getByName(host);
        //优先取URL显式端口，缺省按协议取默认端口；否则IP分支createUnresolved(host,-1)抛port out of range，域名分支忽略显式端口
        int port = uri.getPort() != -1 ? uri.getPort() : (this.ssl ? 443 : 80);
        if (host.equalsIgnoreCase(address.getHostAddress())) {
            // IP地址连接
            inetAddress = InetSocketAddress.createUnresolved(host, port);
        } else {
            // 域名连接
            inetAddress = new InetSocketAddress(address, port);
        }
        return inetAddress;
    }

    private HttpRequest getRequestMethod(String url, Object reqMsg, HttpMethod method, Map<String, String> headers) throws Exception {
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
            if (headers.get(HttpHeaderNames.CONTENT_TYPE.toString()) != null &&
                    HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString().equalsIgnoreCase(headers.get(HttpHeaderNames.CONTENT_TYPE.toString()))) {

                List<BasicNameValuePair> formData = new ArrayList<>();
                Set<Map.Entry<String, Object>> entrySet = JSONObject.parseObject(JSON.toJSONString(reqMsg)).entrySet();
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
                ByteBuf byteBuf = Unpooled.wrappedBuffer(JSON.toJSONBytes(reqMsg));
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
            if (reqMsg != null) {
                Set<Map.Entry<String, Object>> entrySet = JSONObject.parseObject(JSON.toJSONString(reqMsg)).entrySet();
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
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.headers().set(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    private static class OutputResultHandlerCompletable extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final Logger log = LoggerFactory.getLogger(OutputResultHandlerCompletable.class);

        @Setter
        private CompletableFuture<String> response;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            if (!msg.headers().isEmpty()) {
                for (String name : msg.headers().names()) {
                    for (String value : msg.headers().getAll(name)) {
                        if (log.isInfoEnabled()) {
                            log.info("HEADER: {} = {}", name, value);
                        }
                    }
                }
            }

            if (msg.decoderResult().isFailure()) {
                log.error(msg.decoderResult().cause().getMessage());
                response.completeExceptionally(new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败", msg.decoderResult().cause()));
            } else {
                ByteBuf buf = msg.content();
                if (log.isInfoEnabled()) {
                    log.info("content:{}", buf.toString(CharsetUtil.UTF_8));
                }
                response.complete(buf.toString(CharsetUtil.UTF_8));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error(cause.getMessage());
            response.completeExceptionally(new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败", cause));
            ctx.close();
        }
    }

    private static class OutputResultHandlerPromise extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final Logger log = LoggerFactory.getLogger(OutputResultHandlerPromise.class);

        @Setter
        private DefaultPromise<String> response;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            if (!msg.headers().isEmpty()) {
                for (String name : msg.headers().names()) {
                    for (String value : msg.headers().getAll(name)) {
                        if (log.isInfoEnabled()) {
                            log.info("HEADER: {} = {}", name, value);
                        }
                    }
                }
            }

            if (msg.decoderResult().isFailure()) {
                log.error(msg.decoderResult().cause().getMessage());
                response.setFailure(new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败", msg.decoderResult().cause()));
            } else {
                ByteBuf buf = msg.content();
                if (log.isInfoEnabled()) {
                    log.info("content:{}", buf.toString(CharsetUtil.UTF_8));
                }
                response.setSuccess(buf.toString(CharsetUtil.UTF_8));
            }

            //安全回收ByteBuf，此处SimpleChannelInboundHandler会做这个操作没有必要重复。
            //ReferenceCountUtil.safeRelease(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error(cause.getMessage());
            response.setFailure(cause);
            ctx.close();
        }

    }

    private static class OutputResultHandlerAsync extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final Logger log = LoggerFactory.getLogger(OutputResultHandlerAsync.class);

        private final HttpResponseCallback<String> response;

        public OutputResultHandlerAsync(HttpResponseCallback<String> response) {
            this.response = response;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
            if (!msg.headers().isEmpty()) {
                for (String name : msg.headers().names()) {
                    for (String value : msg.headers().getAll(name)) {
                        if (log.isInfoEnabled()) {
                            log.info("HEADER: {} = {}", name, value);
                        }
                    }
                }
            }

            if (msg.decoderResult().isFailure()) {
                log.error(msg.decoderResult().cause().getMessage());
                response.onError(new BizException(ErrorCode.FAIL.getCode(), "调用外部接口失败", msg.decoderResult().cause()));
            } else {
                ByteBuf buf = msg.content();
                if (log.isInfoEnabled()) {
                    log.info("content:{}", buf.toString(CharsetUtil.UTF_8));
                }
                response.onResponse(buf.toString(CharsetUtil.UTF_8));
            }

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error(cause.getMessage());
            response.onError(cause);
            ctx.close();
        }

    }

    /**
     * 实现HttpResponseCallback，异步获取返回结果再执行操作
     */
    public interface HttpResponseCallback<T> {
        /**
         * 处理返回
         *
         * @param response
         */
        void onResponse(T response);

        /**
         * 处理异常
         *
         * @param cause
         */
        void onError(Throwable cause);
    }
}
