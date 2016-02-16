/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.search.model.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Services ENTITY requests from the Engine
 * User: mike
 * Date: 12/04/14
 * Time: 6:23 AM
 */
@Service
@Transactional
@MessageEndpoint
@Configurable
@Profile({"integration","production"})
public class WriteEntityChange {

    private Logger logger = LoggerFactory.getLogger(WriteEntityChange.class);

    @Autowired
    Exchanges exchanges;

    @Autowired
    @Qualifier("esSearchWriter")  // We only suppport ElasticSearch
    SearchWriter searchWriter;

    @Autowired
    AmqpRabbitConfig rabbitConfig;

    @Autowired
    MessageSupport messageSupport;

    private static final ObjectMapper objectMapper = FdJsonObjectMapper.getObjectMapper();

    @Bean
    MessageChannel syncSearchDocs(){
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory) {
        return IntegrationFlows.from(
                Amqp.inboundAdapter(connectionFactory, exchanges.fdSearchQueue())
                    .outputChannel(syncSearchDocs())

                    .maxConcurrentConsumers(exchanges.searchConcurrentConsumers())
                    .prefetchCount(exchanges.searchPreFetchCount())
                )
                .handle(handler())
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = "syncSearchDocs")
    public MessageHandler handler() {
        return message -> {
            try {
                searchWriter.createSearchableChange(objectMapper.readValue((byte[])message.getPayload(), EntitySearchChanges.class));
            } catch (IOException e) {
                logger.error("Unable to de-serialize the payload");
                throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
            }

        };
    }

    @Bean
    @ServiceActivator(inputChannel = "searchDocSyncResult")
    public AmqpOutboundEndpoint writeEntitySearchResult(AmqpTemplate amqpTemplate) {
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
        outbound.setRoutingKey(exchanges.engineBinding());
        outbound.setExchangeName(exchanges.engineExchangeName());
        outbound.setExpectReply(false);
        //outbound.setConfirmAckChannel();
        return outbound;

    }

    @Transformer(inputChannel = "searchReply", outputChannel = "searchDocSyncResult")
    public Message<?> transformMkPayload(Message message) {
        return messageSupport.toJson(message);
    }

    @Profile({"integration","production"})
    @MessagingGateway(name = "engineGateway", asyncExecutor = "fd-search")
    public interface EngineGateway {
        @Gateway(requestChannel = "searchReply", requestTimeout = 40000)
        @Async("fd-search")
        void writeEntitySearchResult(SearchResults searchResult);

    }

}
