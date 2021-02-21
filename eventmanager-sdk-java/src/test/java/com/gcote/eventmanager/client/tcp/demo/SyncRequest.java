package com.gcote.eventmanager.client.tcp.demo;

import com.gcote.eventmanager.client.tcp.WemqAccessClient;
import com.gcote.eventmanager.client.tcp.common.AccessTestUtils;
import com.gcote.eventmanager.client.tcp.common.WemqAccessCommon;
import com.gcote.eventmanager.client.tcp.impl.DefaultWemqAccessClient;
import com.gcote.eventmanager.common.protocol.tcp.UserAgent;
import com.gcote.eventmanager.common.protocol.tcp.Package;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start producer, send message
 */
public class SyncRequest {

    public static Logger logger = LoggerFactory.getLogger(SyncRequest.class);

    private static WemqAccessClient client;

    public static void main(String[] agrs)throws Exception{
        try{
            UserAgent userAgent = AccessTestUtils.generateClient1();
            //client = new DefaultWemqAccessClient("127.0.0.1",10000,userAgent);
            client = new DefaultWemqAccessClient("94.74.68.110",10000,userAgent);
            client.init();
            client.heartbeat();

            Package rrMsg = AccessTestUtils.syncRR();
            logger.info("Enviando mensaje RR sincrono:{}",rrMsg);
            Package response = client.rr(rrMsg, WemqAccessCommon.DEFAULT_TIME_OUT_MILLS);
            logger.info("Recibiendo respuesta RR sincrono:{}",response);

            //Salir destruir recursos
//            client.close();
        }catch (Exception e){
            logger.warn("SyncRequest fallo", e);
        }
    }
}
