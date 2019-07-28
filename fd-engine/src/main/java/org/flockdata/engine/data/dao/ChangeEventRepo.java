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

import java.util.Set;
import org.flockdata.engine.data.graph.ChangeEventNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author mholdsworth
 * @tag GraphRepository, Query, Neo4j, Event
 * @since 28/06/2013
 */
public interface ChangeEventRepo extends GraphRepository<ChangeEventNode> {

  @Query(value = " match (company:FDCompany)-[:COMPANY_EVENT]->(event:Event {code:{1}}) " +
      " where id(company)={0}" +
      "  return event")
  ChangeEventNode findCompanyEvent(Long companyId, String eventName);

  @Query(value =
      "   match (company:FDCompany)-[:COMPANY_EVENT]->events where id(company)={0}" +
          "  return events")
  Set<ChangeEventNode> findCompanyEvents(Long id);
}
