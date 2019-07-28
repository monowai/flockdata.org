/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.integration.engine;

import java.io.IOException;
import javax.annotation.PostConstruct;
import org.flockdata.engine.track.service.SearchHandler;
import org.flockdata.helper.FdJsonObjectMapper;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.Exchanges;
import org.flockdata.search.SearchResults;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

/**
 * fd-search (outbound) to fd-engine (inbound)
 *
 * @author mholdsworth
 * @tag Track, Messaging, Search
 * @since 21/07/2015
 */
@Service
@Profile( {"fd-server"})
public class WriteEntityIndexResult {

  private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = FdJsonObjectMapper.getObjectMapper();
  private final SearchHandler searchHandler;
  private final Exchanges exchanges;
  private ObjectToJsonTransformer transformer;

  @Autowired
  public WriteEntityIndexResult(SearchHandler searchHandler, Exchanges exchanges) {
    this.searchHandler = searchHandler;
    this.exchanges = exchanges;
  }

  @PostConstruct
  public void createTransformer() {
    transformer = new ObjectToJsonTransformer(
        new Jackson2JsonObjectMapper(JsonUtils.getMapper())
    );
    transformer.setContentType(MediaType.APPLICATION_JSON_UTF8.getType());
  }

  public ObjectToJsonTransformer getTransformer() {
    return transformer;
  }

  @Bean
  MessageChannel indexEntityResult() {
    return new DirectChannel();
  }

  @Bean
  public IntegrationFlow writeEntityIndexResultFlow(ConnectionFactory connectionFactory) {
    return IntegrationFlows.from(
        Amqp.inboundAdapter(connectionFactory, exchanges.fdEngineQueue())
            .outputChannel(indexEntityResult())
            .maxConcurrentConsumers(exchanges.engineConcurrentConsumers())
            .prefetchCount(exchanges.enginePreFetchCount())

    )
        .handle(handler())
        .get();
  }

  @Bean
  @ServiceActivator(inputChannel = "indexEntityResult")
  public MessageHandler handler() {
    return message -> {
      try {
        searchHandler.handleResults(objectMapper.readValue(message.getPayload().toString(), SearchResults.class));
      } catch (IOException e) {
        throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
      }
    };
  }

}
