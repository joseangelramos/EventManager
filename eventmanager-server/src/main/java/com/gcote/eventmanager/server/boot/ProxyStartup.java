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

import com.gcote.eventmanager.server.configuration.AccessConfiguration;
import com.gcote.eventmanager.server.configuration.ConfigurationWraper;
import com.gcote.eventmanager.server.configuration.ProxyConfiguration;
import com.gcote.eventmanager.server.constants.ProxyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Clase de Arranque principal para el servidor de eventos
 */
public class ProxyStartup {

    public static Logger logger = LoggerFactory.getLogger(ProxyStartup.class);

    public static void main(String[] args) throws Exception {
        try{
            ConfigurationWraper configurationWraper =
                    new ConfigurationWraper(ProxyConstants.PROXY_CONF_HOME
                            + File.separator
                            + ProxyConstants.PROXY_CONF_FILE, false);
            ProxyConfiguration proxyConfiguration = new ProxyConfiguration(configurationWraper);
            proxyConfiguration.init();
            AccessConfiguration accessConfiguration = new AccessConfiguration(configurationWraper);
            accessConfiguration.init();
            ProxyServer server = new ProxyServer(proxyConfiguration, accessConfiguration);
            server.init();
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("proxy hook de apagado inicia...");
                    long start = System.currentTimeMillis();
                    server.shutdown();
                    long end = System.currentTimeMillis();
                    logger.info("costo de cierre del proxy {}ms", end - start);
                } catch (Exception e) {
                    logger.error("excepcion cuando se apaga....", e);
                }
            }));
        }catch (Throwable e){
            logger.error("fallo arranque del Proxy.", e);
            e.printStackTrace();
        }

    }
}

