package com.gcote.eventmanager.client.tcp.demo;

import com.gcote.eventmanager.client.tcp.WemqAccessClient;
import com.gcote.eventmanager.client.tcp.common.AccessTestUtils;
import com.gcote.eventmanager.client.tcp.common.WemqAccessCommon;
import com.gcote.eventmanager.client.tcp.impl.DefaultWemqAccessClient;
import com.gcote.eventmanager.common.protocol.tcp.UserAgent;
import com.gcote.eventmanager.common.protocol.tcp.Package;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncPublish{

    public static Logger logger = LoggerFactory.getLogger(AsyncPublish.class);

    private static WemqAccessClient client;

    public static AsyncPublish handler = new AsyncPublish();

    public static void main(String[] agrs)throws Exception{
        try{
            UserAgent userAgent = AccessTestUtils.generateClient1();
            client = new DefaultWemqAccessClient("127.0.0.1",10000,userAgent);
            client.init();
            client.heartbeat();

            Package asyncMsg = AccessTestUtils.asyncMessage();
            logger.info("enviando mensaje asincrono：{}", asyncMsg);
            client.publish(asyncMsg, WemqAccessCommon.DEFAULT_TIME_OUT_MILLS);

            Thread.sleep(2000);
            // Salir y cerrar recursos
//            client.close();
        }catch (Exception e){
            logger.warn("AsyncPublish failed", e);
        }
    }
}
