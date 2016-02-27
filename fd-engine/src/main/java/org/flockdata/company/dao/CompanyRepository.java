/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;


public interface CompanyRepository extends GraphRepository<Company> {

    @Query( value =  "match (company:FDCompany)-[r:ACCESSES]- (systemUser:SystemUser) " +
            "where id(company) = {0} and systemUser.login ={1} return systemUser")
    SystemUser getAdminUser(long companyId, String userName);


    @Query(elementClass = Company.class,
            value = "match (su:SystemUser)-[:ACCESSES]->(company:FDCompany) " +
                    "where id(su)={0}" +
                    "return company ")
    Collection<org.flockdata.model.Company> getCompaniesForUser(Long sysUserId);

    @Query(elementClass = Company.class,
            value = "match (su:SystemUser)-[:ACCESSES]->(company:FDCompany) " +
                    "where su.apiKey={0}" +
                    "return company ")
    Collection<org.flockdata.model.Company> findCompanies(String userApiKey);
}
