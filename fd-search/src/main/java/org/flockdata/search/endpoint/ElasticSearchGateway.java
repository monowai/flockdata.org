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

package org.flockdata.search.endpoint;

import org.flockdata.search.model.EntitySearchChange;
import org.flockdata.track.model.Entity;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * User: Mike Holdsworth
 * Since: 9/07/13
 */
@Component
public interface ElasticSearchGateway {
    @Gateway(requestChannel = "sendRequest")
    public void createSearchableChange(EntitySearchChange thisChange);


    //@Gateway(requestChannel = "esDelete")
    public void delete(@Payload Entity entity);
}
