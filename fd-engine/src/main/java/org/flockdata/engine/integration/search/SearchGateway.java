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

import org.flockdata.search.model.*;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * All fd-search requests go here
 * <p>
 * Created by mike on 20/02/16.
 */
@MessagingGateway
public interface SearchGateway {

    //ToDo: I like gateway methods being encapsulated here, but you can only have one @Retryable annotation
    //      so we should move these into their respective Request classes to support retry semantics
    @Payload("new java.util.Date()")
    @Gateway(requestChannel = "searchPing")
    String ping();

    @Gateway(requestChannel = "sendTagCloudRequest", replyChannel = "tagCloudResult")
    TagCloud getTagCloud(TagCloudParams tagCloudParams);

    @Gateway(requestChannel = "sendKeyQuery", replyChannel = "keyResult")
    EntityKeyResults keys(QueryParams queryParams);

    @Gateway(requestChannel = "sendSearchRequest", replyChannel = "fdViewResult")
    EsSearchResult fdSearch(QueryParams queryParams);

    @Gateway(requestChannel = "sendEntityIndexRequest", replyChannel = "nullChannel")
//    @Retryable
    void makeSearchChanges(EntitySearchChanges searchChanges);



}
