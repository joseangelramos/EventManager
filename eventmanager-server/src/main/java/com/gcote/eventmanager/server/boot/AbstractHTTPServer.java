/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcote.eventmanager.server.boot;

import com.google.common.base.Preconditions;
import com.gcote.eventmanager.server.common.Pair;
import com.gcote.eventmanager.server.constants.ProxyConstants;
import com.gcote.eventmanager.server.core.protocol.http.async.AsyncContext;
import com.gcote.eventmanager.server.core.protocol.http.processor.inf.HttpRequestProcessor;
import com.gcote.eventmanager.server.metrics.http.HTTPMetricsServer;
import com.gcote.eventmanager.server.util.ProxyUtil;
import com.gcote.eventmanager.common.ThreadPoolFactory;
import com.gcote.eventmanager.common.command.HttpCommand;
import com.gcote.eventmanager.common.protocol.http.body.Body;
import com.gcote.eventmanager.common.protocol.http.common.ProtocolKey;
import com.gcote.eventmanager.common.protocol.http.common.ProtocolVersion;
import com.gcote.eventmanager.common.protocol.http.common.ProxyRetCode;
import com.gcote.eventmanager.common.protocol.http.common.RequestCode;
import com.gcote.eventmanager.common.protocol.http.header.Header;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractHTTPServer extends AbstractRemotingServer {

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public Logger httpLogger = LoggerFactory.getLogger("http");

    public HTTPMetricsServer metrics;

    public DefaultHttpDataFactory defaultHttpDataFactory = new DefaultHttpDataFactory(false);

    private AtomicBoolean started = new AtomicBoolean(false);

    private boolean useTLS;

    public ThreadPoolExecutor asyncContextCompleteHandler =
            ThreadPoolFactory.createThreadPoolExecutor(10, 10, "proxy-http-asyncContext-");

    static {
        DiskAttribute.deleteOnExitTemporaryFile = false;
    }

    protected HashMap<Integer/* request code */, Pair<HttpRequestProcessor, ThreadPoolExecutor>> processorTable =
            new HashMap<Integer, Pair<HttpRequestProcessor, ThreadPoolExecutor>>(64);

    public AbstractHTTPServer(int port, boolean useTLS) {
        this.port = port;
        this.useTLS = useTLS;
    }

    public Map<String, Object> parseHTTPHeader(HttpRequest fullReq) {
        Map<String, Object> headerParam = new HashMap<>();
        for (String key : fullReq.headers().names()) {
            if (StringUtils.equalsIgnoreCase(HttpHeaderNames.CONTENT_TYPE.toString(), key)
                    || StringUtils.equalsIgnoreCase(HttpHeaderNames.ACCEPT_ENCODING.toString(), key)
                    || StringUtils.equalsIgnoreCase(HttpHeaderNames.CONTENT_LENGTH.toString(), key)) {
                continue;
            }
            headerParam.put(key, fullReq.headers().get(key));
        }
        return headerParam;
    }

    public void sendError(ChannelHandlerContext ctx,
                          HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                status);
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN +
                "; charset=" + ProxyConstants.DEFAULT_CHARSET);
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public void sendResponse(ChannelHandlerContext ctx,
                             DefaultFullHttpResponse response) {
        ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (!f.isSuccess()) {
                    httpLogger.warn("send response to [{}] fail, will close this channel", RemotingHelper.parseChannelRemoteAddr(f.channel()));
                    f.channel().close();
                    return;
                }
            }
        });
    }

    @Override
    public void start() throws Exception {
        super.start();
        Runnable r = () -> {
            ServerBootstrap b = new ServerBootstrap();
            b.group(this.bossGroup, this.workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpsServerInitializer(SSLContextFactory.getSslContext())).childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
            try {
                logger.info("HTTPServer[port={}] started......", this.port);
                ChannelFuture future = b.bind(this.port).sync();
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                logger.error("HTTPServer start Err!", e);
                try {
                    shutdown();
                } catch (Exception e1) {
                    logger.error("HTTPServer shutdown Err!", e);
                }
                return;
            }
        };

        Thread t = new Thread(r, "proxy-http-server");
        t.start();
        started.compareAndSet(false, true);
    }

    @Override
    public void init(String threadPrefix) throws Exception {
        super.init(threadPrefix);
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        started.compareAndSet(true, false);
    }

    public void registerProcessor(Integer requestCode, HttpRequestProcessor processor, ThreadPoolExecutor executor) {
        Preconditions.checkState(ObjectUtils.allNotNull(requestCode), "requestCode can't be null");
        Preconditions.checkState(ObjectUtils.allNotNull(processor), "processor can't be null");
        Preconditions.checkState(ObjectUtils.allNotNull(executor), "executor can't be null");
        Pair<HttpRequestProcessor, ThreadPoolExecutor> pair = new Pair<HttpRequestProcessor, ThreadPoolExecutor>(processor, executor);
        this.processorTable.put(requestCode, pair);
    }

    class HTTPHandler extends SimpleChannelInboundHandler<HttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpRequest httpRequest) throws Exception {
            HttpPostRequestDecoder decoder = null;
            try {
                if (!httpRequest.decoderResult().isSuccess()) {
                    sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                    return;
                }

                final HttpCommand requestCommand = new HttpCommand();

                httpRequest.headers().set(ProtocolKey.ClientInstanceKey.IP, RemotingHelper.parseChannelRemoteAddr(ctx.channel()));

                String protocolVersion = StringUtils.deleteWhitespace(httpRequest.headers().get(ProtocolKey.VERSION));
                if (StringUtils.isBlank(protocolVersion)) {
                    protocolVersion = ProtocolVersion.V1.getVersion();
                    httpRequest.headers().set(ProtocolKey.VERSION, ProtocolVersion.V1.getVersion());
                }

                metrics.summaryMetrics.recordHTTPRequest();

                long bodyDecodeStart = System.currentTimeMillis();

                Map<String, Object> bodyMap = new HashMap<>();

                if (httpRequest.method() == HttpMethod.GET) {
                    QueryStringDecoder getDecoder = new QueryStringDecoder(httpRequest.uri());
                    getDecoder.parameters().entrySet().forEach(entry -> {
                        bodyMap.put(entry.getKey(), entry.getValue().get(0));
                    });
                } else if (httpRequest.method() == HttpMethod.POST) {
                    decoder = new HttpPostRequestDecoder(defaultHttpDataFactory, httpRequest);
                    List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
                    for (InterfaceHttpData parm : parmList) {
                        if (parm.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                            Attribute data = (Attribute) parm;
                            bodyMap.put(data.getName(), data.getValue());
                        }
                    }
                } else {
                    sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                    return;
                }

                metrics.summaryMetrics.recordDecodeTimeCost(System.currentTimeMillis() - bodyDecodeStart);
                String requestCode =
                        (httpRequest.method() == HttpMethod.POST) ? StringUtils.deleteWhitespace(httpRequest.headers().get(ProtocolKey.REQUEST_CODE))
                                : MapUtils.getString(bodyMap, StringUtils.lowerCase(ProtocolKey.REQUEST_CODE), "");

                requestCommand.setHttpMethod(httpRequest.method().name());
                requestCommand.setHttpVersion(httpRequest.protocolVersion().protocolName());
                requestCommand.setRequestCode(requestCode);

                HttpCommand responseCommand = null;

                if (!ProtocolVersion.contains(protocolVersion)) {
                    responseCommand = requestCommand.createHttpCommandResponse(ProxyRetCode.PROXY_PROTOCOL_HEADER_ERR.getRetCode(), ProxyRetCode.PROXY_PROTOCOL_HEADER_ERR.getErrMsg());
                    sendResponse(ctx, responseCommand.httpResponse());
                    return;
                }

                if (StringUtils.isBlank(requestCode)
                        || !StringUtils.isNumeric(requestCode)
                        || !RequestCode.contains(Integer.valueOf(requestCode))
                        || !processorTable.containsKey(Integer.valueOf(requestCode))) {
                    responseCommand = requestCommand.createHttpCommandResponse(ProxyRetCode.PROXY_REQUESTCODE_INVALID.getRetCode(), ProxyRetCode.PROXY_REQUESTCODE_INVALID.getErrMsg());
                    sendResponse(ctx, responseCommand.httpResponse());
                    return;
                }

                if (!started.get()) {
                    responseCommand = requestCommand.createHttpCommandResponse(ProxyRetCode.PROXY_STOP.getRetCode(), ProxyRetCode.PROXY_STOP.getErrMsg());
                    sendResponse(ctx, responseCommand.httpResponse());
                    return;
                }

                try {
                    requestCommand.setHeader(Header.buildHeader(requestCode, parseHTTPHeader(httpRequest)));
                    requestCommand.setBody(Body.buildBody(requestCode, bodyMap));
                } catch (Exception e) {
                    responseCommand = requestCommand.createHttpCommandResponse(ProxyRetCode.PROXY_RUNTIME_ERR.getRetCode(), ProxyRetCode.PROXY_RUNTIME_ERR.getErrMsg() + ProxyUtil.stackTrace(e, 3));
                    sendResponse(ctx, responseCommand.httpResponse());
                    return;
                }

                if (httpLogger.isDebugEnabled()) {
                    httpLogger.debug("{}", requestCommand);
                }

                AsyncContext<HttpCommand> asyncContext = new AsyncContext<HttpCommand>(requestCommand, responseCommand, asyncContextCompleteHandler);
                processProxyRequest(ctx, asyncContext);
            } catch (Exception ex) {
                logger.error("AbrstractHTTPServer.HTTPHandler.channelRead0 err", ex);
            } finally {
                try {
                    decoder.destroy();
                } catch (Exception e) {
                }
            }
        }

        public void processProxyRequest(final ChannelHandlerContext ctx,
                                        final AsyncContext<HttpCommand> asyncContext) {
            final Pair<HttpRequestProcessor, ThreadPoolExecutor> choosed = processorTable.get(Integer.valueOf(asyncContext.getRequest().getRequestCode()));
            try {
                choosed.getObject2().submit(() -> {
                    try {
                        if (choosed.getObject1().rejectRequest()) {
                            HttpCommand responseCommand = asyncContext.getRequest().createHttpCommandResponse(ProxyRetCode.PROXY_REJECT_BY_PROCESSOR_ERROR.getRetCode(), ProxyRetCode.PROXY_REJECT_BY_PROCESSOR_ERROR.getErrMsg());
                            asyncContext.onComplete(responseCommand);
                            if (asyncContext.isComplete()) {
                                if (httpLogger.isDebugEnabled()) {
                                    httpLogger.debug("{}", asyncContext.getResponse());
                                }
                                sendResponse(ctx, responseCommand.httpResponse());
                            }
                            return;
                        }

                        choosed.getObject1().processRequest(ctx, asyncContext);
                        if (asyncContext == null || !asyncContext.isComplete()) {
                            return;
                        }

                        metrics.summaryMetrics.recordHTTPReqResTimeCost(System.currentTimeMillis() - asyncContext.getRequest().getReqTime());

                        if (httpLogger.isDebugEnabled()) {
                            httpLogger.debug("{}", asyncContext.getResponse());
                        }

                        sendResponse(ctx, asyncContext.getResponse().httpResponse());
                    } catch (Exception e) {
                        logger.error("process error", e);
                    }
                });
            } catch (RejectedExecutionException re) {
                HttpCommand responseCommand = asyncContext.getRequest().createHttpCommandResponse(ProxyRetCode.OVERLOAD.getRetCode(), ProxyRetCode.OVERLOAD.getErrMsg());
                asyncContext.onComplete(responseCommand);
                metrics.summaryMetrics.recordHTTPDiscard();
                metrics.summaryMetrics.recordHTTPReqResTimeCost(System.currentTimeMillis() - responseCommand.getReqTime());
                try {
                    sendResponse(ctx, asyncContext.getResponse().httpResponse());
                } catch (Exception e) {
                }
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            super.channelReadComplete(ctx);
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (null != cause) cause.printStackTrace();
            if (null != ctx) ctx.close();
        }
    }

    class HttpConnectionHandler extends ChannelDuplexHandler {
        public AtomicInteger connections = new AtomicInteger(0);

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            int c = connections.incrementAndGet();
            if (c > 20000) {
                logger.warn("client|http|channelActive|remoteAddress={}|msg={}", remoteAddress, "too many client(20000) connect " +
                        "this proxy server");
                ctx.close();
                return;
            }

            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            connections.decrementAndGet();
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            super.channelInactive(ctx);
        }


        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    logger.info("client|http|userEventTriggered|remoteAddress={}|msg={}", remoteAddress, evt.getClass()
                            .getName());
                    ctx.close();
                }
            }

            ctx.fireUserEventTriggered(evt);
        }
    }

    class HttpsServerInitializer extends ChannelInitializer<SocketChannel> {

        private SSLContext sslContext;

        public HttpsServerInitializer(SSLContext sslContext) {
            this.sslContext = sslContext;
        }

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();


            if(sslContext != null && useTLS){
                SSLEngine sslEngine = sslContext.createSSLEngine();
                sslEngine.setUseClientMode(false);
                pipeline.addFirst("ssl", new SslHandler(sslEngine));
            }
            pipeline.addLast(new HttpRequestDecoder(),
                    new HttpResponseEncoder(),
                    new HttpConnectionHandler(),
                    new HttpObjectAggregator(Integer.MAX_VALUE),
                    new HTTPHandler());
        }
    }

}

