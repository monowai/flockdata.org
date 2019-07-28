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

package org.flockdata.engine.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;

/**
 * @author mholdsworth
 * @tag Track, Integration, ExceptionHandler
 * @since 11/11/2014
 */
@IntegrationComponentScan
@Configuration
public class EntityErrorHandler {
  private Logger logger = LoggerFactory.getLogger(EntityErrorHandler.class);

  @Bean
  MessageChannel trackError() {
    return new DirectChannel();
  }

  @Bean
  MessageChannel messagingError() {
    return new DirectChannel();
  }


  @ServiceActivator(inputChannel = "trackError")
  public void handleFailedTrackRequest(Message<MessageHandlingException> message) {
    // ToDo: How to persist failed messages
    MessageHandlingException payLoad = message.getPayload();
    String errorMessage = null;
    if (payLoad != null) {
      Object msgPayload = payLoad.getFailedMessage().getPayload();

      if (payLoad.getCause() != null) {
        errorMessage = payLoad.getCause().getMessage();
      } else {
        errorMessage = payLoad.getMessage();
      }

      if (msgPayload instanceof String) {
        errorMessage = errorMessage + " [" + msgPayload.toString() + "]";
      } else {
        Object o = payLoad.getFailedMessage().getPayload();
        errorMessage = errorMessage + ". " + o.toString();

      }
    }
    logger.error(errorMessage, payLoad);
    throw new AmqpRejectAndDontRequeueException(errorMessage);
    //throw payLoad;
  }

  @ServiceActivator(inputChannel = "messagingError")
  public void handleMessageDeliveryException(Message<MessageDeliveryException> message) {
    // ToDo: How to persist failed messages
    MessageDeliveryException payLoad = message.getPayload();
    String errorMessage;
    if (payLoad.getCause() != null) {
      errorMessage = payLoad.getCause().getMessage();

    } else {
      errorMessage = payLoad.getMessage();
    }
    logger.error(errorMessage, payLoad);

    //throw new AmqpRejectAndDontRequeueException(errorMessage);

  }

}

