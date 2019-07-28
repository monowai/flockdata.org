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

package org.flockdata.engine.integration.store;

import org.flockdata.integration.AmqpRabbitConfig;
import org.flockdata.integration.Exchanges;
import org.flockdata.integration.MessageSupport;
import org.flockdata.store.bean.StorageBean;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Async;

/**
 * @author mholdsworth
 * @since 20/02/2016
 */
@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class StorageWriter {

  @Autowired
  private AmqpRabbitConfig rabbitConfig;

  @Autowired
  private Exchanges exchanges;

  @Autowired
  private MessageSupport messageSupport;

  @Bean
  MessageChannel storeWrite() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel startStoreWrite() {
    return new DirectChannel();
  }

  @Bean
  @ServiceActivator(inputChannel = "storeWrite")
  public AmqpOutboundEndpoint writeToStore(AmqpTemplate amqpTemplate) {
    AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
    outbound.setLazyConnect(rabbitConfig.getAmqpLazyConnect());
    outbound.setRoutingKey(exchanges.storeBinding());
    outbound.setExchangeName(exchanges.fdExchangeName());
    outbound.setExpectReply(false);
    outbound.setConfirmAckChannel(new NullChannel());// NOOP
    //outbound.setConfirmAckChannel();
    return outbound;

  }

  @Transformer(inputChannel = "startStoreWrite", outputChannel = "storeWrite")
  public Message<?> transformMkPayload(Message message) {
    return messageSupport.toJson(message);
  }

  @MessagingGateway
  public interface StorageWriterGateway {
    @Gateway(requestChannel = "startStoreWrite", requestTimeout = 5000, replyChannel = "nullChannel")
    @Async("fd-store")
    void write(StorageBean resultBean);
  }

}
