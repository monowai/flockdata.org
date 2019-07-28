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

package org.flockdata.search.integration;

import java.io.IOException;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flockdata.helper.JsonUtils;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Exchanges;
import org.flockdata.search.AdminRequest;
import org.flockdata.search.SearchChanges;
import org.flockdata.search.base.SearchWriter;
import org.flockdata.search.service.SearchAdmin;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

/**
 * Services ENTITY requests from the Engine
 *
 * @author mholdsworth
 * @tag Search, Entity, Messaging
 * @since 12/04/2014
 */
@Configuration
@IntegrationComponentScan
@Component
@Slf4j
public class InboundSearchHandler {

  // We only support ElasticSearch
  private final SearchWriter searchWriter;
  private final SearchAdmin searchAdmin;
  private Exchanges exchanges;

  @Autowired
  public InboundSearchHandler(SearchAdmin searchAdmin, @Qualifier("esSearchWriter") SearchWriter searchWriter) {
    this.searchAdmin = searchAdmin;
    this.searchWriter = searchWriter;
  }

  @Autowired(required = false)
  void setExchanges(Exchanges exchanges) {
    this.exchanges = exchanges;
  }

  @PostConstruct
  void logStatus() {
    log.info("**** Deployed WriteSearchChanges");
  }

  @Bean
  MessageChannel writeSearchDoc() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel sendSearchResult() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel searchDocSyncResult() {
    return new DirectChannel();
  }

  @Bean
  @Profile("fd-server")
  public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory) {
    return IntegrationFlows.from(
        Amqp.inboundAdapter(connectionFactory, exchanges.fdSearchQueue())
            .outputChannel(writeSearchDoc())
            .mappedRequestHeaders(ClientConfiguration.KEY_MSG_KEY, ClientConfiguration.KEY_MSG_TYPE)
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
        if (oType == null || oType.toString().equalsIgnoreCase("W")) {
          searchWriter.createSearchableChange(JsonUtils.toObject((byte[]) message.getPayload(), SearchChanges.class));
        } else if (oType.toString().equalsIgnoreCase("ADMIN")) {
          AdminRequest adminRequest = JsonUtils.toObject(((String) message.getPayload()).getBytes(), AdminRequest.class);
          searchAdmin.deleteIndexes(adminRequest.getIndexesToDelete());
          log.debug("Got an admin request");
        }
      } catch (IOException e) {
        log.error("Unable to de-serialize the payload. Rejecting due to [{}]", e.getMessage());
        throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
      }

    };
  }


}
