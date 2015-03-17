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

package org.flockdata.engine.query.endpoint;

import org.flockdata.search.model.*;
import org.flockdata.track.model.Entity;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.scheduling.annotation.Async;

/**
 * Facades the call to the underlying auditbucket-search implementation.
 * User: Mike Holdsworth
 * Date: 6/07/13
 * Time: 2:31 PM
 */
@MessagingGateway(asyncExecutor = "fd-engine")
public interface FdSearchGateway {

    @Async("fd-engine")
    @Gateway(requestChannel = "sendEntityIndexRequest",requestTimeout = 10000)
    public void makeSearchChanges(EntitySearchChanges searchChanges);

    @Gateway(requestChannel = "sendSearchRequest", replyChannel = "sendSearchReply" )
    public EsSearchResult search(QueryParams queryParams);

    @Gateway(requestChannel = "sendTagCloudRequest", replyChannel = "sendTagCloudReply")
    public TagCloud getTagCloud(TagCloudParams tagCloudParams);

    public void delete(Entity entity, String searchKey);

    void delete(String indexName);
}
