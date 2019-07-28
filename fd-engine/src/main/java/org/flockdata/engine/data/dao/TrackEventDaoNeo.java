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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.flockdata.data.ChangeEvent;
import org.flockdata.engine.data.graph.ChangeEventNode;
import org.flockdata.engine.data.graph.CompanyNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author mholdsworth
 * @tag Neo4j, Event
 * @since 28/06/2013
 */
@Repository
public class TrackEventDaoNeo {
  final
  private Neo4jTemplate template;

  final
  private ChangeEventRepo eventRepo;

  @Autowired
  public TrackEventDaoNeo(Neo4jTemplate template, ChangeEventRepo eventRepo) {
    this.template = template;
    this.eventRepo = eventRepo;
  }

  //    @Cacheable(value = "companyEvent", unless = "#result == null")
  private ChangeEvent findEvent(CompanyNode company, String eventCode) {
    return eventRepo.findCompanyEvent(company.getId(), eventCode.toLowerCase());
  }

  @Cacheable(value = "companyEvent", unless = "#result == null")
  public ChangeEvent createEvent(CompanyNode company, String eventCode) {
    ChangeEvent ev = findEvent(company, eventCode);
    if (ev == null) {
      String cypher = "merge (event:_Event :Event{code:{code}, name:{name}}) " +
          "with event " +
          "match (c:FDCompany) where id(c) = {coId} " +
          "merge (c)-[:COMPANY_EVENT]->(event) " +
          "return event";

      Map<String, Object> params = new HashMap<>();
      params.put("code", eventCode.toLowerCase());
      params.put("name", eventCode);
      params.put("coId", company.getId());
      Iterable<Map<String, Object>> results = template.query(cypher, params);
      //((Node)row.get("event")).getPropertyKeys();
      for (Map<String, Object> row : results) {
        ev = template.projectTo(row.get("event"), ChangeEventNode.class);
      }
//            ev = findEvent(company, eventCode);
    }

    return ev;
  }

  public Set<ChangeEventNode> findCompanyEvents(Long id) {
    return eventRepo.findCompanyEvents(id);
  }
}
