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

package org.flockdata.kv;

import org.flockdata.helper.JsonUtils;
import org.flockdata.kv.bean.KvContentBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * User: mike
 * Date: 11/11/14
 * Time: 2:14 PM
 */
public class KvErrorHandler {
    private Logger logger = LoggerFactory.getLogger(KvErrorHandler.class);

    @ServiceActivator
    public void handleFailedKvRequest(Message<MessageHandlingException> message) {
        // ToDo: How to persist failed messages
        if (message.getPayload().getFailedMessage().getPayload() instanceof KvContentBean) {
            KvContentBean entity = (KvContentBean) message.getPayload().getFailedMessage().getPayload();
            logger.info(JsonUtils.getJSON(entity));
            logger.error(message.getPayload().getMessage());
        }
        //throw message.getPayload();


    }
}
