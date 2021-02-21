package com.gcote.eventmanager.client.tcp.demo;

import com.gcote.eventmanager.client.tcp.WemqAccessClient;
import com.gcote.eventmanager.client.tcp.common.AccessTestUtils;
import com.gcote.eventmanager.client.tcp.common.ReceiveMsgHook;
import com.gcote.eventmanager.client.tcp.impl.DefaultWemqAccessClient;
import com.gcote.eventmanager.common.protocol.tcp.AccessMessage;
import com.gcote.eventmanager.common.protocol.tcp.UserAgent;
import com.gcote.eventmanager.common.protocol.tcp.Package;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncSubscribeBroadcast implements ReceiveMsgHook {

    public static Logger logger = LoggerFactory.getLogger(AsyncSubscribeBroadcast.class);

    private static WemqAccessClient client;

    public static AsyncSubscribeBroadcast handler = new AsyncSubscribeBroadcast();

    public static void main(String[] agrs)throws Exception{
        try{
            UserAgent userAgent = AccessTestUtils.generateClient2();
            client = new DefaultWemqAccessClient("127.0.0.1",10000,userAgent);
            client.init();
            client.heartbeat();

            client.subscribe("FT0-e-80030000-01-3");
            client.registerSubBusiHandler(handler);

            client.listen();

            //client.unsubscribe();

            //Salir y cerrar recursos
//            client.close();
        }catch (Exception e){
            logger.warn("AsyncSubscribeBroadcast fallo", e);
        }
    }

    @Override
    public void handle(Package msg, ChannelHandlerContext ctx) {
        AccessMessage accessMessage = (AccessMessage)msg.getBody();
        logger.info("sub recibe mensaje de broadcast: {}", accessMessage);
    }
}
