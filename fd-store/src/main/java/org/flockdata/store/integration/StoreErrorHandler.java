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

import org.flockdata.helper.FlockDataTagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * KV messages should always be written
 * <p>
 * To get here, we have gone through a Retry advice chain so likely the DB is down
 * The message will stay on the q as unacknowledged and redelivery will be attempted
 * and the process will begin again.
 *
 * @author mholdsworth
 * @tag Store, ExceptionHandler
 * @since 11/11/2014
 */
@IntegrationComponentScan
public class StoreErrorHandler {
  private Logger logger = LoggerFactory.getLogger(StoreErrorHandler.class);

  @ServiceActivator(inputChannel = "storeErrors")
  public void handleFailedKvRequest(Message<MessageHandlingException> message) {
    MessageHandlingException payLoad = message.getPayload();
    if (payLoad.getCause() != null) {
      logger.error(payLoad.getCause().getMessage());
      if (payLoad.getCause() instanceof FlockDataTagException) {
        return; // Log and get out of here
      }
    } else {
      logger.error(payLoad.getMessage());
    }

    throw payLoad;


  }
}
