/*
 * Copyright 2013 by Maxim Kalina
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.javaforge.netty.servlet.bridge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import net.javaforge.netty.servlet.bridge.impl.FilterChainImpl;
import net.javaforge.netty.servlet.bridge.impl.HttpServletRequestImpl;
import net.javaforge.netty.servlet.bridge.impl.HttpServletResponseImpl;
import net.javaforge.netty.servlet.bridge.impl.ServletBridgeWebapp;
import net.javaforge.netty.servlet.bridge.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class ServletBridgeHandler extends IdleStateHandler {


    private static final Logger log = LoggerFactory
            .getLogger(ServletBridgeHandler.class);

    private List<ServletBridgeInterceptor> interceptors;


    /**
     * Which uri should be passed into this servlet container
     */
    private String uriPrefix = "/";

    public ServletBridgeHandler() {
        this("/");
    }

    public ServletBridgeHandler(String uriPrefix) {
        super(20000, 20000, 20000);
        this.uriPrefix = uriPrefix;
    }

    public ServletBridgeHandler addInterceptor(
            ServletBridgeInterceptor interceptor) {

        if (interceptors == null)
            interceptors = new ArrayList<ServletBridgeInterceptor>();

        interceptors.add(interceptor);
        return this;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        log.debug("Opening new channel: {}", ctx.channel().id());
        ServletBridgeWebapp.get().getSharedChannelGroup().add(ctx.channel());

        ctx.fireChannelActive();
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) {
        log.debug("Closing idle channel: {}", ctx.channel().id());
        ctx.channel().close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object e)
            throws Exception {

        if (e instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) e;

            String uri = request.uri();

            if (uri.startsWith(uriPrefix)) {
                if (HttpHeaders.is100ContinueExpected(request)) {
                    ctx.channel().write(new DefaultHttpResponse(HTTP_1_1, CONTINUE));
                }


                FilterChainImpl chain = ServletBridgeWebapp.get().initializeChain(uri);

                if (chain.isValid()) {
                    handleHttpServletRequest(ctx, request, chain);
                } else if (ServletBridgeWebapp.get().getStaticResourcesFolder() != null) {
                    handleStaticResourceRequest(ctx, request);
                } else {
                    throw new ServletBridgeRuntimeException(
                            "No handler found for uri: " + request.getUri());
                }
            } else {
                ctx.fireChannelRead(e);
            }
        } else {
            ctx.fireChannelRead(e);
        }
    }

    protected void handleHttpServletRequest(ChannelHandlerContext ctx,
                                            HttpRequest request, FilterChainImpl chain) throws Exception {

        interceptOnRequestReceived(ctx, request);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);

        HttpServletResponseImpl resp = buildHttpServletResponse(response);
        HttpServletRequestImpl req = buildHttpServletRequest(request, chain);


        chain.doFilter(req, resp);

        interceptOnRequestSuccessed(ctx, request, response);

        resp.getWriter().flush();

        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        if (keepAlive) {

            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // -
            // http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // write response...
        ChannelFuture future = ctx.channel().writeAndFlush(response);

        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

    }

    protected void handleStaticResourceRequest(ChannelHandlerContext ctx,
                                               HttpRequest request) throws Exception {
        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        String uri = Utils.sanitizeUri(request.uri());
        final String path = (uri != null ? ServletBridgeWebapp.get()
                .getStaticResourcesFolder().getAbsolutePath()
                + File.separator + uri : null);

        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        setContentLength(response, fileLength);

        Channel ch = ctx.channel();

        // Write the initial line and the header.
        ch.write(response);

        // Write the content.
        ChannelFuture writeFuture;
        if (isSslChannel(ch)) {
            // Cannot use zero-copy with HTTPS.
            writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
        } else {
            // No encryption - use zero-copy.
            final FileRegion region = new DefaultFileRegion(raf.getChannel(),
                    0, fileLength);
            writeFuture = ch.write(region);
            writeFuture.addListener(new ChannelProgressiveFutureListener() {

                @Override
                public void operationProgressed(ChannelProgressiveFuture channelProgressiveFuture, long current, long total) throws Exception {
                    System.out.printf("%s: %d / %d (+%d)%n", path, current,
                            total, total);
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture channelProgressiveFuture) throws Exception {
                    region.release();
                }
            });
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Unexpected exception from downstream.", cause);

        Channel ch = ctx.channel();
        if (cause instanceof IllegalArgumentException) {
            ch.close();
        } else {
            if (cause instanceof TooLongFrameException) {
                sendError(ctx, BAD_REQUEST);
                return;
            }

            if (ch.isActive()) {
                sendError(ctx, INTERNAL_SERVER_ERROR);
            }

        }

    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        String text = "Failure: " + status.toString() + "\r\n";
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = null;
        try {
            bytes = text.getBytes("utf-8");
            byteBuf.writeBytes(bytes);
        } catch (UnsupportedEncodingException e) {
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        HttpHeaders headers = response.headers();

        headers.add(CONTENT_TYPE, "text/plain;charset=utf-8");
        headers.add(CACHE_CONTROL, "no-cache");
        headers.add(PRAGMA, "No-cache");
        headers.add(SERVER, "eBay Server");
        headers.add(CONTENT_LENGTH, byteBuf.readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void interceptOnRequestReceived(ChannelHandlerContext ctx,
                                            HttpRequest request) {
        if (interceptors != null) {
            for (ServletBridgeInterceptor interceptor : interceptors) {
                interceptor.onRequestReceived(ctx, request);
            }
        }

    }

    private void interceptOnRequestSuccessed(ChannelHandlerContext ctx,
                                             HttpRequest request, HttpResponse response) {
        if (interceptors != null) {
            for (ServletBridgeInterceptor interceptor : interceptors) {
                interceptor.onRequestSuccessed(ctx, request, response);
            }
        }

    }

    // private void interceptOnRequestFailed(ChannelHandlerContext ctx,
    // ExceptionEvent e, HttpResponse response) {
    // if (this.interceptors != null) {
    // for (ServletBridgeInterceptor interceptor : this.interceptors) {
    // interceptor.onRequestFailed(ctx, e, response);
    // }
    // }
    //
    // }

    protected HttpServletResponseImpl buildHttpServletResponse(
            FullHttpResponse response) {
        return new HttpServletResponseImpl(response);
    }

    protected HttpServletRequestImpl buildHttpServletRequest(
            HttpRequest request, FilterChainImpl chain) {
        return new HttpServletRequestImpl(request, chain);
    }

    private boolean isSslChannel(Channel ch) {
        return ch.pipeline().get(SslHandler.class) != null;
    }

    public String getUriPrefix() {
        return uriPrefix;
    }

    public void setUriPrefix(String uriPrefix) {
        this.uriPrefix = uriPrefix;
    }
}
