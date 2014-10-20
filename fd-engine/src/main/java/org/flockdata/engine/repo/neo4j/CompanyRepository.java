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

package org.flockdata.engine.repo.neo4j;

import org.flockdata.registration.dao.neo4j.model.CompanyNode;
import org.flockdata.registration.dao.neo4j.model.SystemUserNode;
import org.flockdata.registration.model.Company;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;


public interface CompanyRepository extends GraphRepository<CompanyNode> {

    @Query(elementClass = SystemUserNode.class, value = "start company=node({0}) " +
            "match company-[r:ACCESSES]-systemUser " +
            "where systemUser.login ={1} return systemUser")
    SystemUserNode getAdminUser(long ID, String userName);


    @Query(elementClass = CompanyNode.class,
            value = "match (su:SystemUser)-[:ACCESSES]->(company:ABCompany) " +
                    "where id(su)={0}" +
                    "return company ")
    Collection<Company> getCompaniesForUser(Long sysUserId);

    @Query(elementClass = CompanyNode.class,
            value = "match (su:SystemUser)-[:ACCESSES]->(company:ABCompany) " +
                    "where su.apiKey={0}" +
                    "return company ")
    Collection<Company> findCompanies(String userApiKey);
}
