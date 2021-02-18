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

package com.gcote.eventmanager.server.core.protocol.http.processor;

import com.gcote.eventmanager.server.boot.ProxyServer;
import com.gcote.eventmanager.server.constants.ProxyConstants;
import com.gcote.eventmanager.server.core.protocol.http.async.AsyncContext;
import com.gcote.eventmanager.server.core.protocol.http.processor.inf.HttpRequestProcessor;
import com.gcote.eventmanager.common.IPUtil;
import com.gcote.eventmanager.common.command.HttpCommand;
import com.gcote.eventmanager.common.protocol.http.common.ProxyRetCode;
import com.gcote.eventmanager.common.protocol.http.common.RequestCode;
import io.netty.channel.ChannelHandlerContext;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminShutdownProcessor implements HttpRequestProcessor {

    public Logger cmdLogger = LoggerFactory.getLogger("cmd");

    private ProxyServer proxyServer;

    public AdminShutdownProcessor(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, AsyncContext<HttpCommand> asyncContext) throws Exception {

        HttpCommand responseProxyCommand;
        cmdLogger.info("cmd={}|{}|client2proxy|from={}|to={}", RequestCode.get(Integer.valueOf(asyncContext.getRequest().getRequestCode())),
                ProxyConstants.PROTOCOL_HTTP,
                RemotingHelper.parseChannelRemoteAddr(ctx.channel()), IPUtil.getLocalAddress());

        proxyServer.shutdown();

        responseProxyCommand = asyncContext.getRequest().createHttpCommandResponse(
                ProxyRetCode.SUCCESS.getRetCode(), ProxyRetCode.SUCCESS.getErrMsg());
        asyncContext.onComplete(responseProxyCommand);
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
