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

import java.util.Collection;
import org.flockdata.data.Company;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.data.graph.SystemUserNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;


public interface CompanyRepository extends GraphRepository<CompanyNode> {

  @Query(elementClass = SystemUserNode.class, value = "match (company:FDCompany)-[r:ACCESSES]- (systemUser:SystemUser) " +
      "where id(company) = {0} and systemUser.login ={1} return systemUser")
  SystemUserNode getAdminUser(long companyId, String userName);


  @Query(elementClass = CompanyNode.class,
      value = "match (su:SystemUser)-[:ACCESSES]->(company:FDCompany) " +
          "where id(su)={0}" +
          "return company ")
  Collection<Company> getCompaniesForUser(Long sysUserId);

  @Query(elementClass = CompanyNode.class,
      value = "match (su:SystemUser)-[:ACCESSES]->(company:FDCompany) " +
          "where su.apiKey={0}" +
          "return company ")
  Collection<Company> findCompanies(String userApiKey);
}
