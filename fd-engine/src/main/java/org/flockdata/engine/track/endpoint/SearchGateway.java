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

package org.flockdata.engine.track.endpoint;

import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.SearchChange;
import org.springframework.integration.annotation.Gateway;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * User: Mike Holdsworth
 * Date: 7/07/13
 * Time: 8:54 AM
 */
public interface SearchGateway {

    @Gateway(requestChannel = "makeSearchRequest")
    public SearchChange createSearchableChange(@Payload EntitySearchChanges changes);

    @Gateway(requestChannel = "searchDelete")
    public void delete(@Payload Entity entity);

    //ToDo:
    // Add an HTTP gateway call to ElasticSearchEP.getMetaKeys(QueryParams);
}
