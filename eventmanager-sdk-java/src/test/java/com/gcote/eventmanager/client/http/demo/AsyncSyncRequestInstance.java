package com.gcote.eventmanager.client.http.demo;
import com.gcote.eventmanager.client.http.conf.LiteClientConfig;
import com.gcote.eventmanager.client.http.producer.LiteProducer;
import com.gcote.eventmanager.client.http.producer.RRCallback;
import com.gcote.eventmanager.common.IPUtil;
import com.gcote.eventmanager.common.LiteMessage;
import com.gcote.eventmanager.common.ThreadUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncSyncRequestInstance {

    public static Logger logger = LoggerFactory.getLogger(SyncRequestInstance.class);

    public static void main(String[] args) throws Exception {

        LiteProducer liteProducer = null;
        try {
            String proxyIPPort = args[0];

            final String topic = args[1];

            if (StringUtils.isBlank(proxyIPPort)) {
                // si es multivalor asi se configura 127.0.0.1:10105;127.0.0.2:10105
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

            final long startTime = System.currentTimeMillis();
            final LiteMessage liteMessage = new LiteMessage();
            liteMessage.setBizSeqNo(RandomStringUtils.randomNumeric(30))
                    .setContent("contentStr con un protocolo especial")
                    .setTopic(topic)
                    .setUniqueId(RandomStringUtils.randomNumeric(30));

            liteProducer.request(liteMessage, new RRCallback() {
                @Override
                public void onSuccess(LiteMessage o) {
                    logger.debug("sendmsg : {}, return : {}, tiempo:{}ms", liteMessage.getContent(), System.currentTimeMillis() - startTime);
                }

                @Override
                public void onException(Throwable e) {
                    logger.debug("sendmsg fallo", e);
                }
            }, 3000);

            Thread.sleep(2000);
        } catch (Exception e) {
            logger.warn("envio de mensaje asincrono fallo", e);
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
