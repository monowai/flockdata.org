/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.engine.track;

import org.flockdata.engine.track.model.EntityTagIn;
import org.flockdata.engine.track.model.EntityTagOut;
import org.flockdata.track.model.EntityTag;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;

/**
 * There is an EntityTagIn and EntityTagOut class because of the way SDN handles inbound and outbound relationships
 * This repo should be the central place to work on queries for EntityTag objects, but don't use it for persistence
 *
 * Created by mike on 6/04/15.
 */
public interface EntityTagRepo extends GraphRepository<EntityTagOut> {
    @Query (elementClass = EntityTagOut.class, value = "match (e:Entity)-[r]->(:Tag) where id(e) = {0} return r")
    Collection<EntityTag> getEntityTagsOut(Long entityId);

    @Query (elementClass = EntityTagIn.class, value = "match (e:Entity)<-[r]-(:Tag) where id(e) = {0} return r")
    Collection<EntityTag> getEntityTagsIn(Long entityId);

}
