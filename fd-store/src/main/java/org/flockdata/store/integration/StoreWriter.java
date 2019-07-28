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

package org.flockdata.store.integration;

import static org.flockdata.helper.FdJsonObjectMapper.getObjectMapper;

import java.io.IOException;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.Exchanges;
import org.flockdata.store.bean.StorageBean;
import org.flockdata.store.service.StoreService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Inbound handler to write data to a store
 *
 * @author mholdsworth
 * @tag Integration, Store
 * @since 17/02/2016
 */
@Configuration
@IntegrationComponentScan
@Profile( {"fd-server"})
public class StoreWriter {

  @Autowired(required = false)
  private Exchanges exchanges;
  @Autowired
  private StoreService fdStoreManager;

  @Bean
  MessageChannel startStoreWrite() {
    return new DirectChannel();
  }

  @Bean
  public IntegrationFlow writeEntityChangeFlow(ConnectionFactory connectionFactory, RetryOperationsInterceptor storeInterceptor)
      throws Exception {

    return IntegrationFlows.from(
        Amqp.inboundAdapter(connectionFactory, exchanges.fdStoreQueue())
            .outputChannel(startStoreWrite())
//                        .adviceChain(storeInterceptor)
//                        .maxConcurrentConsumers(exchanges.storeConcurrentConsumers())
//                        .prefetchCount(exchanges.storePreFetchCount())
    )
        .handle(handler())
        .get();
  }

  @Bean
  @ServiceActivator(inputChannel = "startStoreWrite")
  public MessageHandler handler() throws Exception {
    return message -> {
      try {
        fdStoreManager.doWrite(
            getObjectMapper().readValue((byte[]) message.getPayload(), StorageBean.class));
      } catch (IOException e) {
        //logger.error("Unable to de-serialize the payload");
        throw new AmqpRejectAndDontRequeueException("Unable to de-serialize the payload", e);
      } catch (FlockException e) {
        throw new AmqpRejectAndDontRequeueException("Error writing the payload", e);
      }

    };
  }


}
