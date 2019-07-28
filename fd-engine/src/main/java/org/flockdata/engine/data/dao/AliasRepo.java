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
import org.flockdata.data.Alias;
import org.flockdata.engine.data.graph.AliasNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mholdsworth
 * @tag GraphRepository, Alias, Neo4j
 * @since 30/06/2013
 */
public interface AliasRepo extends GraphRepository<AliasNode> {
  @Query(elementClass = AliasNode.class, value =
      "match (t) -[:HAS_ALIAS]->(alias) where id(t)={0}  return alias")
  Collection<Alias> findTagAliases(Long id);


}
