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

import java.util.List;
import org.flockdata.engine.data.graph.FortressNode;
import org.flockdata.engine.data.graph.FortressUserNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface FortressRepository extends GraphRepository<FortressNode> {

  @Query(value = " match (fortress:Fortress)<-[:BELONGS_TO]-(fortress@author FortressUser) where id(fortress)={0}"
      + " and fortressUser.code ={1} return fortressUser")
  FortressUserNode getFortressUser(Long fortressId, String userName);

  @Query(elementClass = FortressNode.class, value = " match (company:FDCompany)-[:OWNS]->f where id(company) ={0} return f")
  List<FortressNode> findCompanyFortresses(Long companyID);

  @Query(value = "match (company:FDCompany)-[r:OWNS]->(fortress:Fortress) "
      + "where id(company)={0} and fortress.name ={1} "
      + "return fortress")
  FortressNode getFortressByName(Long companyId, String fortressName);

  @Query(value = "match (company:FDCompany)-[r:OWNS]->(fortress:Fortress) "
      + "where  id(company)={0}  and fortress.code ={1} "
      + "return fortress")
  FortressNode getFortressByCode(Long companyId, String fortressCode);

}
