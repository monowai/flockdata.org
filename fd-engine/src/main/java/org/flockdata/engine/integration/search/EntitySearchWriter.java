/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.integration.search;

import org.flockdata.search.model.SearchChanges;
import org.flockdata.shared.AmqpRabbitConfig;
import org.flockdata.shared.Exchanges;
import org.flockdata.shared.MessageSupport;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * Created by mike on 12/02/16.
 */
@Configuration
@IntegrationComponentScan
@Profile({"fd-server"})
public class EntitySearchWriter {

    @Autowired
    AmqpRabbitConfig rabbitConfig;

    @Autowired
    Exchanges exchanges;
    @Autowired
    MessageSupport messageSupport;

    @MessagingGateway
    public interface EntitySearchWriterGateway {
        @Gateway(requestChannel = "sendEntityIndexRequest", replyChannel = "nullChannel")
        void makeSearchChanges(SearchChanges searchChanges);
    }


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
