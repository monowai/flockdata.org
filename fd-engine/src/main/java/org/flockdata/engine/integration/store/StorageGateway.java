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

package org.flockdata.engine.integration.store;

import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.bean.StorageBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Retryable;

/**
 * Created by mike on 20/02/16.
 */
@MessagingGateway
@Configuration
public interface StorageGateway {
    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "storePing")
    String ping();

    @Payload("#args")
    @Gateway(requestChannel = "startStoreRead", requestTimeout = 40000, replyChannel = "storeReadResult")
    StoredContent read(Store store, String index, String type, String key);

    @Gateway(requestChannel = "startStoreWrite", requestTimeout = 40000, replyChannel = "nullChannel")
    @Retryable
//    @Async("fd-store")
    void write(StorageBean resultBean);

}
