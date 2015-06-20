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

package org.flockdata.company.dao;

import org.flockdata.company.model.CompanyNode;
import org.flockdata.registration.model.SystemUser;
import org.flockdata.registration.dao.CompanyDao;
import org.flockdata.registration.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 10:05 PM
 */
@Repository
public class CompanyDaoNeo implements CompanyDao {

    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    Neo4jTemplate template;

    @Override
    public Company update(Company company) {
        return companyRepo.save((CompanyNode) company);
    }

    @Override
    public Company findByPropertyValue(String property, Object value) {
        return companyRepo.findBySchemaPropertyValue(property, value);
    }

    @Override
    public Collection<Company> findCompanies(Long sysUserId) {
        return companyRepo.getCompaniesForUser(sysUserId);
    }

    @Override
    public Collection<Company> findCompanies(String userApiKey) {
        return companyRepo.findCompanies(userApiKey);
    }

    @Override
    public Company create(Company company) {

        return companyRepo.save((CompanyNode) company);
    }


    @Override
    public SystemUser getAdminUser(Long companyId, String name) {
        return companyRepo.getAdminUser(companyId, name);
    }

    @Override
    public Company create(String companyName, String uniqueKey) {
        return create(new CompanyNode(companyName, uniqueKey));
    }

}
