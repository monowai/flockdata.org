package org.flockdata.engine.integration.store;

import org.flockdata.engine.integration.AmqpRabbitConfig;
import org.flockdata.engine.integration.Exchanges;
import org.flockdata.engine.integration.MessageSupport;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * Created by mike on 20/02/16.
 */
@Configuration
@IntegrationComponentScan
public class StorageWriter {

    @Autowired
    AmqpRabbitConfig rabbitConfig;

    @Autowired
    Exchanges exchanges;

    @Autowired
    MessageSupport messageSupport;

    @Bean
    MessageChannel storeWrite(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel startStoreWrite(){
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "storeWrite")
    public AmqpOutboundEndpoint writeToStore(AmqpTemplate amqpTemplate){
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
        outbound.setRoutingKey(exchanges.storeBinding());
        outbound.setExchangeName(exchanges.storeExchange());
        outbound.setExpectReply(false);
        outbound.setConfirmAckChannel(new NullChannel());// NOOP
        //outbound.setConfirmAckChannel();
        return outbound;

    }

    @Transformer(inputChannel= "startStoreWrite", outputChannel="storeWrite")
    public Message<?> transformMkPayload(Message message){
        return messageSupport.toJson(message);
    }

}