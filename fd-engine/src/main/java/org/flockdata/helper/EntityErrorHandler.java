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

package org.flockdata.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;

/**
 * User: mike
 * Date: 11/11/14
 * Time: 2:14 PM
 */
public class EntityErrorHandler {
    private Logger logger = LoggerFactory.getLogger(EntityErrorHandler.class);

    @ServiceActivator
    public void handleFailedTrackRequest(Message<MessageHandlingException> message) {
        // ToDo: How to persist failed messages
        MessageHandlingException payLoad = message.getPayload();
        if (payLoad != null) {
            Object msgPayload = payLoad.getFailedMessage().getPayload();
            String errorMessage ;
            if (payLoad.getCause() != null) {
                errorMessage = payLoad.getCause().getMessage();
                logger.error(payLoad.getCause().getMessage());
            }else
                errorMessage = payLoad.getMessage();

            if (msgPayload instanceof String)
                logger.debug(errorMessage + " [" +msgPayload.toString()+"]");
            else {
                Object o = payLoad.getFailedMessage().getPayload();
                logger.info(errorMessage + ". " + o.toString());

            }
        }
        throw payLoad;


    }
    @ServiceActivator
    public void handleMessageDeliveryException(Message<MessageDeliveryException> message) {
        // ToDo: How to persist failed messages
        MessageDeliveryException payLoad = message.getPayload();
        Object msgPayload = payLoad.getFailedMessage().getPayload();
        if ( payLoad.getCause()!= null ) {
            logger.error(payLoad.getCause().getMessage());
            if ( payLoad.getCause() instanceof FlockDataTagException){
                return; // Log and get out of here
            }

        } else
            logger.error(payLoad.getMessage());

        if (msgPayload instanceof String)
            logger.debug(msgPayload.toString());
        else {
            Object o = ((MessageDeliveryException) msgPayload).getFailedMessage().getPayload();
            logger.info(o.toString());

        }

        throw payLoad;

    }

}

