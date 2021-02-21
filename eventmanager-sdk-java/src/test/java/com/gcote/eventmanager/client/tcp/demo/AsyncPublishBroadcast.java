package com.gcote.eventmanager.client.tcp.demo;

import com.gcote.eventmanager.client.tcp.WemqAccessClient;
import com.gcote.eventmanager.client.tcp.common.AccessTestUtils;
import com.gcote.eventmanager.client.tcp.common.WemqAccessCommon;
import com.gcote.eventmanager.client.tcp.impl.DefaultWemqAccessClient;
import com.gcote.eventmanager.common.protocol.tcp.UserAgent;
import com.gcote.eventmanager.common.protocol.tcp.Package;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncPublishBroadcast {

    public static Logger logger = LoggerFactory.getLogger(AsyncPublishBroadcast.class);

    private static WemqAccessClient client;

    public static void main(String[] agrs)throws Exception{
        try{
            UserAgent userAgent = AccessTestUtils.generateClient1();
            client = new DefaultWemqAccessClient("127.0.0.1",10000,userAgent);
            client.init();
            client.heartbeat();

            Package broadcastMsg = AccessTestUtils.broadcastMessage();
            logger.info("enviando mensaje broadcast: {}", broadcastMsg);
            client.broadcast(broadcastMsg, WemqAccessCommon.DEFAULT_TIME_OUT_MILLS);

            Thread.sleep(2000);
            // salir y cerrar recursos
//            client.close();
        }catch (Exception e){
            logger.warn("AsyncPublishBroadcast fallo", e);
        }
    }
}
