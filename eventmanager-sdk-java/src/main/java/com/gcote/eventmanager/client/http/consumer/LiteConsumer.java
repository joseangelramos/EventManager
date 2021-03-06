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

package com.gcote.eventmanager.client.http.consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.gcote.eventmanager.client.http.AbstractLiteClient;
import com.gcote.eventmanager.client.http.RemotingServer;
import com.gcote.eventmanager.client.http.conf.LiteClientConfig;
import com.gcote.eventmanager.client.http.consumer.listener.LiteMessageListener;
import com.gcote.eventmanager.common.ProxyException;
import com.gcote.eventmanager.common.ThreadPoolFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiteConsumer extends AbstractLiteClient {

    public Logger logger = LoggerFactory.getLogger(LiteConsumer.class);

    private RemotingServer remotingServer;

    private ThreadPoolExecutor consumeExecutor;

    private static CloseableHttpClient httpClient = HttpClients.createDefault();

    protected LiteClientConfig weMQProxyClientConfig;

    private List<String> subscription = Lists.newArrayList();

    private LiteMessageListener messageListener;

    public LiteConsumer(LiteClientConfig liteClientConfig) {
        super(liteClientConfig);
        this.consumeExecutor = ThreadPoolFactory.createThreadPoolExecutor(weMQProxyClientConfig.getConsumeThreadCore(),
                weMQProxyClientConfig.getConsumeThreadMax(), "proxy-client-consume-");
        this.remotingServer = new RemotingServer(consumeExecutor);
    }

    public LiteConsumer(LiteClientConfig liteClientConfig,
                        ThreadPoolExecutor customExecutor) {
        super(liteClientConfig);
        this.consumeExecutor = customExecutor;
        this.remotingServer = new RemotingServer(this.consumeExecutor);
    }

    private AtomicBoolean started = new AtomicBoolean(Boolean.FALSE);

    public void start() throws ProxyException {
        Preconditions.checkState(weMQProxyClientConfig != null, "weMQProxyClientConfig no puede ser nulo");
        Preconditions.checkState(consumeExecutor != null, "consumeExecutor no puede ser nulo");
        Preconditions.checkState(messageListener != null, "messageListener no puede ser nulo");
        logger.info("LiteConsumer iniciando");
        super.start();
        started.compareAndSet(false, true);
        logger.info("LiteConsumer iniciado");
    }

    public void shutdown() throws Exception {
        logger.info("LiteConsumer finalizando");
        super.shutdown();
        httpClient.close();
        started.compareAndSet(true, false);
        logger.info("LiteConsumer apagado");
    }


    public boolean subscribe(String topic) throws ProxyException {
        subscription.add(topic);
        //hearbeat
        return Boolean.TRUE;
    }

    public boolean unsubscribe(String topic) throws ProxyException {
        subscription.remove(topic);
        //hearbeat
        return Boolean.TRUE;
    }

    public void registerMessageListener(LiteMessageListener messageListener) throws ProxyException {
        this.messageListener = messageListener;
        remotingServer.registerMessageListener(this.messageListener);
    }
}
