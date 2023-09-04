/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.netty;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kdla.framework.common.help.ThreadPoolHelp;
import top.kdla.framework.supplement.http.ssl.SslContextFactory;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author kanglele
 * @version $Id: NettyHttpServer, v 0.1 2023/6/13 16:17 kanglele Exp $
 */
@Slf4j
public class NettyHttpServer {

    private boolean keepAlive = true;
    private final DispatcherServer dispatcherServer;
    private boolean ssl = false;
    private File server;
    private File servertruststore;
    private String keyStorePassword;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap serverBootstrap;

    public NettyHttpServer(DispatcherServer dispatcherServer, boolean keepAlive) {
        this.dispatcherServer = dispatcherServer;
        this.keepAlive = keepAlive;

        ThreadPoolHelp threadPoolHelp = new ThreadPoolHelp();
        this.bossGroup = new NioEventLoopGroup(1, threadPoolHelp.getDefaultExecutorService());
        this.workerGroup = new NioEventLoopGroup(1, threadPoolHelp.getDefaultExecutorService());
        this.serverBootstrap = new ServerBootstrap();

    }

    public NettyHttpServer setSsl(File server, File servertruststore, String keyStorePassword) {
        this.ssl = true;
        this.server = server;
        this.servertruststore = servertruststore;
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public void start(int port) throws Exception {
        AtomicBoolean close = new AtomicBoolean(false);
        try {
            this.serverBootstrap.group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, this.keepAlive)
                    .childHandler(new HttpServerInitializer(this.ssl, this.server, this.servertruststore, keyStorePassword, this.dispatcherServer));
            ChannelFuture f = serverBootstrap.bind(port);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        log.info("服务端口" + port + "绑定成功!");
                    }
                }
            });
            //f.channel().closeFuture().sync();//不建议使用阻塞方式
            f.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                    close.set(true);
                    log.info(future.channel().toString() + "链路关闭");
                }
            });
        } finally {
            if (!close.get()) {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }

    private static class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
        private final boolean ssl;
        private final File server;
        private final File servertruststore;
        private final String keyStorePassword;
        private final DispatcherServer dispatcherServer;

        public HttpServerInitializer(boolean ssl, File server, File servertruststore, String keyStorePassword, DispatcherServer dispatcherServer) {
            this.ssl = ssl;
            this.server = server;
            this.servertruststore = servertruststore;
            this.keyStorePassword = keyStorePassword;
            this.dispatcherServer = dispatcherServer;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            if (this.ssl) {
                //配置Https通信
                //开发测试模式
//                SelfSignedCertificate ssc = new SelfSignedCertificate();
//                SslContext context = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//                SSLEngine sslEngine = context.newEngine(ch.alloc());
                //生产模式
                SSLEngine sslEngine = SslContextFactory.getServerContext(server, servertruststore, keyStorePassword).createSSLEngine();
                sslEngine.setUseClientMode(false);
                ch.pipeline().addFirst("ssl", new SslHandler(sslEngine));
            }
            ch.pipeline().addLast(new HttpServerCodec());//是HttpRequestDecoder和HttpResponseEncoder的封装
            //Http请求经过HttpServerCodec解码之后是HttpRequest和HttpContents对象，
            //HttpObjectAggregator会将多个HttpRequest和HttpContents对象再拼装成一个FullHttpRequest，再将其传递到下个Handler
            ch.pipeline().addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024));
            ch.pipeline().addLast(new HttpServerInboundHandlerJson(this.dispatcherServer));
        }
    }

    private static class HttpServerInboundHandlerJson extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final Logger log = LoggerFactory.getLogger(HttpServerInboundHandlerJson.class);

        private final DispatcherServer dispatcherServer;

        public HttpServerInboundHandlerJson(DispatcherServer dispatcherServer) {
            this.dispatcherServer = dispatcherServer;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            log.info("客服端地址:{}", ctx.channel().remoteAddress());
            log.info("请求数据:{}", JSONObject.toJSONString(msg));
            HttpHeaders headers = msg.headers();
            String headValue = headers.get(HttpHeaderNames.CONTENT_TYPE.toString());
            String uri = msg.uri();
            HttpMethod method = msg.method();
            log.info("Uri:{} method:{} headValue:{}", uri, method, headers.toString());
            if (method.equals(HttpMethod.GET)) {
                QueryStringDecoder decoderQuery = new QueryStringDecoder(msg.uri());
                Map<String, List<String>> uriAttributes = decoderQuery.parameters();
                try {
                    Object res = dispatcherServer.doDispatch(uri, uriAttributes);
                    writeHttpResponse(msg, headValue, res, ctx, HttpResponseStatus.OK);
                } catch (Exception e) {
                    writeHttpResponse(msg, headValue, ExceptionUtils.getStackTrace(e), ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                }

            } else if (method.equals(HttpMethod.POST)) {
                ByteBuf buf = msg.content();
                String bodyString = buf.toString(Charsets.UTF_8);
                try {
                    Object res = dispatcherServer.doDispatch(uri, bodyString);
                    writeHttpResponse(msg, headValue, res, ctx, HttpResponseStatus.OK);
                } catch (Exception e) {
                    writeHttpResponse(msg, headValue, ExceptionUtils.getStackTrace(e), ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                }

            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

        }

        private void writeHttpResponse(FullHttpRequest request, String headValue, Object res, ChannelHandlerContext ctx, HttpResponseStatus status) throws Exception {
            if (StringUtils.isNotBlank(headValue) && headValue.equalsIgnoreCase(HttpHeaderValues.TEXT_HTML.toString())) {
                if (res instanceof File) {
                    writeHttpResponseHtml(request, (File) res, ctx, status);
                } else {
                    writeHttpResponseHtml2(request, JSONObject.toJSONString(res), ctx, status);
                }
            } else if (StringUtils.isNotBlank(headValue) && headValue.equalsIgnoreCase(HttpHeaderValues.TEXT_PLAIN.toString())) {
                writeHttpResponsePlain(request, JSONObject.toJSONString(res), ctx, status);
            } else if (StringUtils.isNotBlank(headValue) && "image/png".equalsIgnoreCase(headValue)) {
                writeHttpResponseImage(request, (File) res, ctx, status);
            } else {
                writeHttpResponseJson(request, JSONObject.toJSONString(res), ctx, status);
            }
        }

        private void writeHttpResponseJson(FullHttpRequest request, String res, ChannelHandlerContext ctx, HttpResponseStatus status) {
            log.info("writeHttpResponseJson 开始写入返回数据...");
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.wrappedBuffer(res.getBytes(Charsets.UTF_8)));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().setInt(HttpHeaderNames.EXPIRES, 0);
            // 检查请求是否为 keep-alive
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            // 在响应头中设置 keep-alive
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }

        private void writeHttpResponseImage(FullHttpRequest request, File file, ChannelHandlerContext ctx, HttpResponseStatus status) {
            log.info("writeHttpResponseImage 开始写入返回数据...");
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);
            //byte[] fileToByte = this.fileToByte("f://test.jpg");
            response.content().writeBytes(FileUtil.readBytes(file));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/png;charset=utf-8");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().writerIndex());
            // 检查请求是否为 keep-alive
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            // 在响应头中设置 keep-alive
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }

        private void writeHttpResponseHtml(FullHttpRequest request, File file, ChannelHandlerContext ctx, HttpResponseStatus status) throws Exception {
            log.info("writeHttpResponseHtml 开始写入返回数据...");
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().writerIndex());
            ctx.write(response);
            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()));
            // 检查请求是否为 keep-alive
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            // 在响应头中设置 keep-alive
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            } else {
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
            }
        }

        private void writeHttpResponseHtml2(FullHttpRequest request, String msg, ChannelHandlerContext ctx, HttpResponseStatus status) throws Exception {
            log.info("writeHttpResponseHtml2 开始写入返回数据...");
            //2.给浏览器进行响应
            ByteBuf byteBuf = Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, byteBuf);
            //2.1 设置响应头
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());
            // 检查请求是否为 keep-alive
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            // 在响应头中设置 keep-alive
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }

        private void writeHttpResponsePlain(FullHttpRequest request, String msg, ChannelHandlerContext ctx, HttpResponseStatus status) throws Exception {
            log.info("writeHttpResponsePlain 开始写入返回数据...");
            // 构造一个http响应体，即HttpResponse
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
            // 设置响应头信息
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            // 将响应体写入到通道中
            // 检查请求是否为 keep-alive
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            // 在响应头中设置 keep-alive
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error(cause.getMessage());
            ctx.close();
        }
    }

    /**
     * 请求转向匹配服务
     *
     * @param <T>
     */
    @FunctionalInterface
    public interface DispatcherServer<T> {
        /**
         * 转向匹配
         *
         * @param key
         * @param t
         */
        Object doDispatch(String key, T t);

    }
}
