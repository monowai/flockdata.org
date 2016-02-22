package org.flockdata.engine.integration.search;

import org.flockdata.engine.integration.AmqpRabbitConfig;
import org.flockdata.engine.integration.Exchanges;
import org.flockdata.engine.integration.MessageSupport;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * Created by mike on 12/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"integration","production"})
public class EntitySearchWriter {

    @Autowired
    AmqpRabbitConfig rabbitConfig;

    @Autowired
    Exchanges exchanges;
    @Autowired
    MessageSupport messageSupport;

    // ToDo: Can we handle this more via the flow or handler?
    @Transformer(inputChannel="sendEntityIndexRequest", outputChannel="writeSearchChanges")
    public Message<?> transformSearchChanges(Message theObject){
        return messageSupport.toJson(theObject);
    }

    @Bean
    MessageChannel writeSearchChanges(){
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "writeSearchChanges")
    public AmqpOutboundEndpoint fdSearchAMQPOutbound(AmqpTemplate amqpTemplate) {
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
        outbound.setRoutingKey(exchanges.searchBinding());
        outbound.setExchangeName(exchanges.searchExchange());
        outbound.setExpectReply(false);
        outbound.setConfirmAckChannel(new NullChannel());// NOOP
        //outbound.setConfirmAckChannel();
        return outbound;

    }


}
