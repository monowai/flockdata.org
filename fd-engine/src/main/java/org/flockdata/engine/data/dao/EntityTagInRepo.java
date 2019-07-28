/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

package org.flockdata.engine.data.dao;

import java.util.Collection;
import org.flockdata.data.EntityTag;
import org.flockdata.engine.data.graph.EntityTagIn;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mholdsworth
 * @tag Neo4j, GraphRepository, Relationship, EntityTag
 * @since 6/04/2015
 */
public interface EntityTagInRepo extends GraphRepository<EntityTagIn> {

  @Query(elementClass = EntityTagIn.class, value = "match (e:Entity)<-[r]-(:Tag) where id(e) = {0} return r")
  Collection<EntityTag> getEntityTags(Long entityId);

}
