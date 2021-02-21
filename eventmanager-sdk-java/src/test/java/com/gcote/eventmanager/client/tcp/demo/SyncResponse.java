package com.gcote.eventmanager.client.tcp.demo;

import com.gcote.eventmanager.client.tcp.WemqAccessClient;
import com.gcote.eventmanager.client.tcp.common.AccessTestUtils;
import com.gcote.eventmanager.client.tcp.common.ReceiveMsgHook;
import com.gcote.eventmanager.client.tcp.impl.DefaultWemqAccessClient;
import com.gcote.eventmanager.common.protocol.tcp.UserAgent;
import com.gcote.eventmanager.common.protocol.tcp.Package;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * start consumer ,subscribe topic
 */
public class SyncResponse implements ReceiveMsgHook {

    public static Logger logger = LoggerFactory.getLogger(SyncResponse.class);

    private static WemqAccessClient client;

    public static SyncResponse handler = new SyncResponse();

    public static void main(String[] agrs)throws Exception{
        try{
            UserAgent userAgent = AccessTestUtils.generateClient2();
            client = new DefaultWemqAccessClient("94.74.68.110",10000,userAgent);
            //client = new DefaultWemqAccessClient("127.0.0.1",10000,userAgent);
            client.init();
            client.heartbeat();

            client.subscribe("FT0-s-80000000-01-0");
            //Sincronizar mensajes RR
            client.registerSubBusiHandler(handler);

            client.listen();

            //client.unsubscribe();

            //Salir, destruir recursos
//            client.close();
        }catch (Exception e){
            logger.warn("SyncResponse fallo", e);
        }
    }

    @Override
    public void handle(Package msg, ChannelHandlerContext ctx) {
        logger.info("mensaje subscriber recibido：{}", msg);
        Package pkg = AccessTestUtils.rrResponse(msg);
        ctx.writeAndFlush(pkg);
    }
}
