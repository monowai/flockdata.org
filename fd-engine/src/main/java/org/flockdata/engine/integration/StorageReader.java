package org.flockdata.engine.integration;

import org.flockdata.engine.PlatformConfig;
import org.flockdata.helper.JsonUtils;
import org.flockdata.store.bean.StorageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.IOException;

/**
 * Handles reading content from fd-store
 * Created by mike on 17/02/16.
 */
@IntegrationComponentScan
@Configuration
@Profile({"integration","production"})
public class StorageReader {

    @Autowired
    @Qualifier("engineConfig")
    PlatformConfig engineConfig;

    @Bean
    MessageChannel startStoreRead(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel storeReadResult(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel receiveStoreReadReply() {
        return new DirectChannel();
    }

    @Bean
    IntegrationFlow storeFlow() {

        return IntegrationFlows.from(startStoreRead())
                .handle(storeRead())
                .get();
    }

    private MessageHandler storeRead() {
        HttpRequestExecutingMessageHandler handler =
                new HttpRequestExecutingMessageHandler(engineConfig.getFdStore()+ "/v1/data/{store}/{index}/{type}/{key}");
        handler.setExpectedResponseType(StorageBean.class);
        handler.setHttpMethod(HttpMethod.GET);
        handler.setOutputChannel(receiveStoreReadReply());
        return handler;
    }

    @Transformer(inputChannel="receiveStoreReadReply", outputChannel="storeReadResult")
    public StorageBean receiveStoreReadReply(Message<String> theObject) throws IOException {
        return JsonUtils.toObject(theObject.getPayload().getBytes(), StorageBean.class);
    }

}
