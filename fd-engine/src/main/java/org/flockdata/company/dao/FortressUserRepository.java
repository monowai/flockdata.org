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

package org.flockdata.company.dao;

import org.flockdata.engine.data.graph.FortressUserNode;
import org.flockdata.engine.data.graph.SystemUserNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface FortressUserRepository extends GraphRepository<FortressUserNode> {

  @Query(value = "match (fortress:Fortress)<-[r:BELONGS_TO]-(f@author FortressUser) where id(fortress)={0} match fUser.name ={1} return fUser")
  SystemUserNode getAdminUser(long fortressId, String userName);

  @Query(
      value = "match (sys@author SystemUser {name: {0}}-[:ACCESSES]->(company:FDCompany)-[:OWNS]->(fortress:Fortress)<-[:BELONGS_TO]-(fortress@author FortressUser) " +
          "where fortressUser.name ={2} and fortress.name={1} return fortressUser")
  FortressUserNode getFortressUser(String userName, String fortressName, String fortressUser);


}
