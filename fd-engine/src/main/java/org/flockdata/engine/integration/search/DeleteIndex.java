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

import java.util.Collections;
import java.util.Map;
import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.ClientConfiguration;
import org.flockdata.integration.Exchanges;
import org.flockdata.integration.MessageSupport;
import org.flockdata.search.AdminRequest;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author mholdsworth
 * @tag Configuration, Integration, Search, Administration
 * @since 12/02/2016
 */
@Configuration
@IntegrationComponentScan
@Profile("fd-server")
public class DeleteIndex {

  @Autowired
  private AmqpRabbitConfig rabbitConfig;

  @Autowired
  private Exchanges exchanges;

  @Autowired
  private MessageSupport messageSupport;

  @Transformer(inputChannel = "startIndexDelete", outputChannel = "adminHeadersChannel")
  public Message<?> deleteSearchIndex(Message message) {
    return messageSupport.toJson(message);
  }

  @Bean
  @Transformer(inputChannel = "adminHeadersChannel", outputChannel = "writeAdminChanges")
  public HeaderEnricher enrichHeaders() {
    Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd =
        Collections.singletonMap(ClientConfiguration.KEY_MSG_TYPE,
            new StaticHeaderValueMessageProcessor<>("admin"));
    return new HeaderEnricher(headersToAdd);
  }

  @Bean
  MessageChannel startIndexDelete() {
    return new DirectChannel();
  }

  @Bean
  @ServiceActivator(inputChannel = "writeAdminChanges")
  public AmqpOutboundEndpoint fdWriteAdminChanges(AmqpTemplate amqpTemplate) {
    AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
    outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
    outbound.setRoutingKey(exchanges.searchBinding());
    outbound.setExchangeName(exchanges.fdExchangeName());
    DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
    headerMapper.setRequestHeaderNames(ClientConfiguration.KEY_MSG_TYPE);
    outbound.setHeaderMapper(headerMapper);
    outbound.setExpectReply(false);
    outbound.setConfirmAckChannel(new NullChannel());// NOOP
    //outbound.setConfirmAckChannel();
    return outbound;

  }


  @MessagingGateway
  public interface DeleteIndexGateway {
    @Gateway(requestChannel = "startIndexDelete", requestTimeout = 5000, replyChannel = "nullChannel")
//        @Async("fd-search")
    void deleteIndex(AdminRequest adminRequest);
  }
}
