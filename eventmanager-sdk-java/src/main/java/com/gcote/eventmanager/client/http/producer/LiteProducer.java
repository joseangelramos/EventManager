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

package com.gcote.eventmanager.client.http.producer;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.gcote.eventmanager.client.http.AbstractLiteClient;
import com.gcote.eventmanager.client.http.ProxyRetObj;
import com.gcote.eventmanager.client.http.conf.LiteClientConfig;
import com.gcote.eventmanager.client.http.http.HttpUtil;
import com.gcote.eventmanager.client.http.http.RequestParam;
import com.gcote.eventmanager.client.http.ssl.MyX509TrustManager;
import com.gcote.eventmanager.common.Constants;
import com.gcote.eventmanager.common.LiteMessage;
import com.gcote.eventmanager.common.ProxyException;
import com.gcote.eventmanager.common.protocol.http.body.message.SendMessageRequestBody;
import com.gcote.eventmanager.common.protocol.http.body.message.SendMessageResponseBody;
import com.gcote.eventmanager.common.protocol.http.common.ProtocolKey;
import com.gcote.eventmanager.common.protocol.http.common.ProtocolVersion;
import com.gcote.eventmanager.common.protocol.http.common.ProxyRetCode;
import com.gcote.eventmanager.common.protocol.http.common.RequestCode;
import io.netty.handler.codec.http.HttpMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiteProducer extends AbstractLiteClient {

    public Logger logger = LoggerFactory.getLogger(LiteProducer.class);

    private static CloseableHttpClient httpClient = HttpClients.createDefault();

    public LiteProducer(LiteClientConfig liteClientConfig) {
        super(liteClientConfig);
        if(liteClientConfig.isUseTls()){
            setHttpClient();
        }
    }

    private AtomicBoolean started = new AtomicBoolean(Boolean.FALSE);

    public void start() throws ProxyException {
        Preconditions.checkState(liteClientConfig != null, "liteClientConfig no puede ser nulo");
        Preconditions.checkState(liteClientConfig.getLiteProxyAddr() != null, "liteClientConfig.liteServerAddr no puede ser nulo");
        if(started.get()) {
            return;
        }
        logger.info("LiteProducer iniciando");
        super.start();
        started.compareAndSet(false, true);
        logger.info("LiteProducer iniciado");
    }

    public void shutdown() throws Exception {
        if(!started.get()) {
            return;
        }
        logger.info("LiteProducer finalizando");
        super.shutdown();
        httpClient.close();
        started.compareAndSet(true, false);
        logger.info("LiteProducer apagado");
    }

    public AtomicBoolean getStarted() {
        return started;
    }

    public boolean publish(LiteMessage message) throws ProxyException {
        if (!started.get()) {
            start();
        }
        Preconditions.checkState(StringUtils.isNotBlank(message.getTopic()),
                "proxyMessage[topic] invalido");
        Preconditions.checkState(StringUtils.isNotBlank(message.getContent()),
                "proxyMessage[content] invalido");
        RequestParam requestParam = new RequestParam(HttpMethod.POST);
        requestParam.addHeader(ProtocolKey.REQUEST_CODE, String.valueOf(RequestCode.MSG_SEND_ASYNC.getRequestCode()))
                .addHeader(ProtocolKey.ClientInstanceKey.ENV, liteClientConfig.getEnv())
                .addHeader(ProtocolKey.ClientInstanceKey.REGION, liteClientConfig.getRegion())
                .addHeader(ProtocolKey.ClientInstanceKey.IDC, liteClientConfig.getIdc())
                .addHeader(ProtocolKey.ClientInstanceKey.DCN, liteClientConfig.getDcn())
                .addHeader(ProtocolKey.ClientInstanceKey.IP, liteClientConfig.getIp())
                .addHeader(ProtocolKey.ClientInstanceKey.PID, liteClientConfig.getPid())
                .addHeader(ProtocolKey.ClientInstanceKey.SYS, liteClientConfig.getSys())
                .addHeader(ProtocolKey.ClientInstanceKey.USERNAME, liteClientConfig.getUserName())
                .addHeader(ProtocolKey.ClientInstanceKey.PASSWD, liteClientConfig.getPassword())
                .addHeader(ProtocolKey.VERSION, ProtocolVersion.V1.getVersion())
                .addHeader(ProtocolKey.LANGUAGE, Constants.LANGUAGE_JAVA)
                .setTimeout(Constants.DEFAULT_HTTP_TIME_OUT)
                .addBody(SendMessageRequestBody.TOPIC, message.getTopic())
                .addBody(SendMessageRequestBody.CONTENT, message.getContent())
                .addBody(SendMessageRequestBody.TTL, message.getPropKey(Constants.PROXY_MESSAGE_CONST_TTL))
                .addBody(SendMessageRequestBody.BIZSEQNO, message.getBizSeqNo())
                .addBody(SendMessageRequestBody.UNIQUEID, message.getUniqueId());

        long startTime = System.currentTimeMillis();
        String target = selectProxy();
        String res = "";
        try {
            res = HttpUtil.post(httpClient, target, requestParam);
        } catch (Exception ex) {
            throw new ProxyException(ex);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("publicando mensaje asincrono, targetProxy:{}, tiempo:{}ms, mensaje:{}, rtn:{}",
                    target, System.currentTimeMillis() - startTime, message, res);
        }

        ProxyRetObj ret = JSON.parseObject(res, ProxyRetObj.class);

        if (ret.getRetCode() == ProxyRetCode.SUCCESS.getRetCode()) {
            return Boolean.TRUE;
        } else {
            throw new ProxyException(ret.getRetCode(), ret.getRetMsg());
        }
    }

    public String selectProxy() {
        if (CollectionUtils.isEmpty(proxyServerList)) {
            return null;
        }
        if(liteClientConfig.isUseTls()){
            return Constants.HTTPS_PROTOCOL_PREFIX + proxyServerList.get(RandomUtils.nextInt(0, proxyServerList.size()));
        }else{
            return Constants.HTTP_PROTOCOL_PREFIX + proxyServerList.get(RandomUtils.nextInt(0, proxyServerList.size()));
        }
    }

    public LiteMessage request(LiteMessage message, long timeout) throws ProxyException {
        if(!started.get()) {
            start();
        }
        Preconditions.checkState(StringUtils.isNotBlank(message.getTopic()),
                "proxyMessage[topic] invalido");
        Preconditions.checkState(StringUtils.isNotBlank(message.getContent()),
                "proxyMessage[content] invalido");
        RequestParam requestParam = new RequestParam(HttpMethod.POST);
        requestParam.addHeader(ProtocolKey.REQUEST_CODE, String.valueOf(RequestCode.MSG_SEND_SYNC.getRequestCode()))
                .addHeader(ProtocolKey.ClientInstanceKey.ENV, liteClientConfig.getEnv())
                .addHeader(ProtocolKey.ClientInstanceKey.REGION, liteClientConfig.getRegion())
                .addHeader(ProtocolKey.ClientInstanceKey.IDC, liteClientConfig.getIdc())
                .addHeader(ProtocolKey.ClientInstanceKey.DCN, liteClientConfig.getDcn())
                .addHeader(ProtocolKey.ClientInstanceKey.IP, liteClientConfig.getIp())
                .addHeader(ProtocolKey.ClientInstanceKey.PID, liteClientConfig.getPid())
                .addHeader(ProtocolKey.ClientInstanceKey.SYS, liteClientConfig.getSys())
                .addHeader(ProtocolKey.ClientInstanceKey.USERNAME, liteClientConfig.getUserName())
                .addHeader(ProtocolKey.ClientInstanceKey.PASSWD, liteClientConfig.getPassword())
                .addHeader(ProtocolKey.VERSION, ProtocolVersion.V1.getVersion())
                .addHeader(ProtocolKey.LANGUAGE, Constants.LANGUAGE_JAVA)
                .setTimeout(timeout)
                .addBody(SendMessageRequestBody.TOPIC, message.getTopic())
                .addBody(SendMessageRequestBody.CONTENT, message.getContent())
                .addBody(SendMessageRequestBody.TTL, String.valueOf(timeout))
                .addBody(SendMessageRequestBody.BIZSEQNO, message.getBizSeqNo())
                .addBody(SendMessageRequestBody.UNIQUEID, message.getUniqueId());

        long startTime = System.currentTimeMillis();
        String target = selectProxy();
        String res = "";
        try {
            res = HttpUtil.post(httpClient, target, requestParam);
        } catch (Exception ex) {
            throw new ProxyException(ex);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("publicando mensaje sincrono por esperar, targetProxy:{}, tiempo:{}ms, message:{}, rtn:{}", target, System.currentTimeMillis() - startTime, message, res);
        }

        ProxyRetObj ret = JSON.parseObject(res, ProxyRetObj.class);
        if (ret.getRetCode() == ProxyRetCode.SUCCESS.getRetCode()) {
            LiteMessage proxyMessage = new LiteMessage();
            SendMessageResponseBody.ReplyMessage replyMessage =
                    JSON.parseObject(ret.getRetMsg(), SendMessageResponseBody.ReplyMessage.class);
            proxyMessage.setContent(replyMessage.body).setProp(replyMessage.properties)
                    .setTopic(replyMessage.topic);
            return proxyMessage;
        }

        return null;
    }

    public void request(LiteMessage message, RRCallback rrCallback, long timeout) throws ProxyException {
        if(!started.get()) {
            start();
        }
        Preconditions.checkState(StringUtils.isNotBlank(message.getTopic()),
                "proxyMessage[topic] invalido");
        Preconditions.checkState(StringUtils.isNotBlank(message.getContent()),
                "proxyMessage[content] invalido");
        Preconditions.checkState(ObjectUtils.allNotNull(rrCallback),
                "rrCallback invalido");
        RequestParam requestParam = new RequestParam(HttpMethod.POST);
        requestParam.addHeader(ProtocolKey.REQUEST_CODE, String.valueOf(RequestCode.MSG_SEND_SYNC.getRequestCode()))
                .addHeader(ProtocolKey.ClientInstanceKey.ENV, liteClientConfig.getEnv())
                .addHeader(ProtocolKey.ClientInstanceKey.REGION, liteClientConfig.getRegion())
                .addHeader(ProtocolKey.ClientInstanceKey.IDC, liteClientConfig.getIdc())
                .addHeader(ProtocolKey.ClientInstanceKey.DCN, liteClientConfig.getDcn())
                .addHeader(ProtocolKey.ClientInstanceKey.IP, liteClientConfig.getIp())
                .addHeader(ProtocolKey.ClientInstanceKey.PID, liteClientConfig.getPid())
                .addHeader(ProtocolKey.ClientInstanceKey.SYS, liteClientConfig.getSys())
                .addHeader(ProtocolKey.ClientInstanceKey.USERNAME, liteClientConfig.getUserName())
                .addHeader(ProtocolKey.ClientInstanceKey.PASSWD, liteClientConfig.getPassword())
                .addHeader(ProtocolKey.VERSION, ProtocolVersion.V1.getVersion())
                .addHeader(ProtocolKey.LANGUAGE, Constants.LANGUAGE_JAVA)
                .setTimeout(timeout)
                .addBody(SendMessageRequestBody.TOPIC, message.getTopic())
                .addBody(SendMessageRequestBody.CONTENT, message.getContent())
                .addBody(SendMessageRequestBody.TTL, String.valueOf(timeout))
                .addBody(SendMessageRequestBody.BIZSEQNO, message.getBizSeqNo())
                .addBody(SendMessageRequestBody.UNIQUEID, message.getUniqueId());

        long startTime = System.currentTimeMillis();
        String target = selectProxy();
        try {
            HttpUtil.post(httpClient, null, target, requestParam, new RRCallbackResponseHandlerAdapter(message, rrCallback, timeout));
        } catch (Exception ex) {
            throw new ProxyException(ex);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("publicando mensaje esperando respuesta asincrona, target:{}, tiempo:{}, message:{}", target, System.currentTimeMillis() - startTime, message);
        }
    }

    public static void setHttpClient() {
        SSLContext sslContext = null;
        try {
            String protocol = System.getProperty("ssl.client.protocol", "TLSv1.1");
            TrustManager[] tm = new TrustManager[] { new MyX509TrustManager() };
            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, tm, new SecureRandom());
            httpClient = HttpClients.custom().setSslcontext(sslContext)
                    .setHostnameVerifier(SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER).build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
