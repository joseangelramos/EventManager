package com.gcote.eventmanager.client.http.demo;

import com.gcote.eventmanager.client.http.conf.LiteClientConfig;
import com.gcote.eventmanager.client.http.producer.LiteProducer;
import com.gcote.eventmanager.common.Constants;
import com.gcote.eventmanager.common.IPUtil;
import com.gcote.eventmanager.common.LiteMessage;
import com.gcote.eventmanager.common.ThreadUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncPublishInstance {

    public static Logger logger = LoggerFactory.getLogger(AsyncPublishInstance.class);

    public static void main(String[] args) throws Exception {

        LiteProducer liteProducer = null;
        try{
            String proxyIPPort = args[0];

            String topic = args[1];

            if (StringUtils.isBlank(proxyIPPort)) {
                // isi es multivalor asi se configura: 127.0.0.1:10105;127.0.0.2:10105
                proxyIPPort = "127.0.0.1:10105";
            }

            LiteClientConfig weMQProxyClientConfig = new LiteClientConfig();
            weMQProxyClientConfig.setLiteProxyAddr(proxyIPPort)
                    .setEnv("env")
                    .setIdc("idc")
                    .setDcn("dcn")
                    .setIp(IPUtil.getLocalAddress())
                    .setSys("1234")
                    .setPid(String.valueOf(ThreadUtil.getPID()));

            liteProducer = new LiteProducer(weMQProxyClientConfig);
            liteProducer.start();

            LiteMessage liteMessage = new LiteMessage();
            liteMessage.setBizSeqNo(RandomStringUtils.randomNumeric(30))
                    .setContent("contentStr con protocolo espeacial")
                    .setTopic(topic)
                    .setUniqueId(RandomStringUtils.randomNumeric(30))
                    .addProp(Constants.PROXY_MESSAGE_CONST_TTL, String.valueOf(4 * 3600 * 1000));

            boolean flag = liteProducer.publish(liteMessage);
            Thread.sleep(1000);
            logger.info("resultado publicado, {}", flag);
        }catch (Exception e){
            logger.warn("fallo mensaje publicado", e);
        }

        try{
            Thread.sleep(30000);
            if(liteProducer != null){
                liteProducer.shutdown();
            }
        }catch (Exception e1){
            logger.warn("producer shutdown exception", e1);
        }
    }
}
