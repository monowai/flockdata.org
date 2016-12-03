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

package org.flockdata.search.integration;

import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Exchanges;
import org.flockdata.integration.MessageSupport;
import org.flockdata.search.AdminRequest;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.model.SearchChanges;
import org.flockdata.search.model.SearchResults;
import org.flockdata.search.service.SearchAdmin;
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
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Services ENTITY requests from the Engine
 * @author mholdsworth
 * @since 12/04/2014
 * @tag Search, Entity, Messaging
 */
@Transactional
@MessageEndpoint
@Configurable
@Profile({"fd-server"})
public class WriteSearchChanges {

    private final Exchanges exchanges;
    // We only support ElasticSearch
    private final SearchWriter searchWriter;
    private final SearchAdmin searchAdmin;
    private final AmqpRabbitConfig rabbitConfig;
    private final MessageSupport messageSupport;
    private Logger logger = LoggerFactory.getLogger(WriteSearchChanges.class);

    @Autowired
    public WriteSearchChanges(AmqpRabbitConfig rabbitConfig, MessageSupport messageSupport, Exchanges exchanges, SearchAdmin searchAdmin, @Qualifier("esSearchWriter") SearchWriter searchWriter) {
        this.rabbitConfig = rabbitConfig;
        this.messageSupport = messageSupport;
        this.exchanges = exchanges;
        this.searchAdmin = searchAdmin;
        this.searchWriter = searchWriter;
    }

//    private static final ObjectMapper objectMapper = FdJsonObjectMapper.getObjectMapper();

    @Bean
    MessageChannel writeSearchDoc(){
        return new DirectChannel();
    }

    @Bean
    MessageChannel searchReply () {
        return new DirectChannel();
    }

    @Bean
    MessageChannel sendSearchResult() {
        return new DirectChannel();
    }

    @Bean
    MessageChannel searchDocSyncResult () {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory, RetryOperationsInterceptor searchInterceptor) {
        return IntegrationFlows.from(
                Amqp.inboundAdapter(connectionFactory, exchanges.fdSearchQueue())
                    .outputChannel(writeSearchDoc())
                        .mappedRequestHeaders(ClientConfiguration.KEY_MSG_KEY, ClientConfiguration.KEY_MSG_TYPE)
                        .adviceChain(searchInterceptor)
                        .maxConcurrentConsumers(exchanges.searchConcurrentConsumers())
                    .prefetchCount(exchanges.searchPreFetchCount())
                )
                .handle(handler())
                .get();
    }

    @Bean
    @ServiceActivator(inputChannel = "writeSearchDoc")
    public MessageHandler handler() {
        return message -> {
            try {
                Object oType = message.getHeaders().get(ClientConfiguration.KEY_MSG_TYPE);
                if ( oType == null || oType.toString().equalsIgnoreCase("W"))
                    searchWriter.createSearchableChange(JsonUtils.toObject((byte[])message.getPayload(), SearchChanges.class));
                else if ( oType.toString().equalsIgnoreCase("ADMIN")){
                    AdminRequest adminRequest =JsonUtils.toObject(((String)message.getPayload()).getBytes(),AdminRequest.class);
                    searchAdmin.deleteIndexes(adminRequest.getIndexesToDelete());
                    logger.info("Got an admin request");
                }
            } catch (IOException e) {
                logger.error("Unable to de-serialize the payload. Rejecting due to [{}]", e.getMessage());
                throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
            }

        };
    }

    @Transformer(inputChannel = "searchReply", outputChannel = "searchDocSyncResult")
    public Message<?> transformSearchResults( Message message) {
        return messageSupport.toJson(message);
    }

    @Bean
    @ServiceActivator(inputChannel = "searchDocSyncResult")
    public AmqpOutboundEndpoint writeEntitySearchResult(AmqpTemplate amqpTemplate) {
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
        outbound.setExchangeName(exchanges.fdExchangeName());
        outbound.setRoutingKey(exchanges.fdEngineBinding());
        outbound.setExpectReply(false);
        return outbound;

    }

    @MessagingGateway(name = "engineGateway", asyncExecutor = "fd-search")
    public interface EngineResultGateway {
        @Gateway(requestChannel = "searchReply", requestTimeout = 40000)
        @Async("fd-search")
        void writeEntitySearchResult(SearchResults searchResult);

    }


}
