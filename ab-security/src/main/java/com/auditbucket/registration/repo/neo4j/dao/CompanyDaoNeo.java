/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.registration.repo.neo4j.dao;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.CompanyRepository;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 10:05 PM
 */
@Repository
public class CompanyDaoNeo implements CompanyDao {
    private static final String FORTRESS_NAME = "fortressName";
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
    public SystemUser getAdminUser(Long companyId, String name) {
        return companyRepo.getAdminUser(companyId, name);
    }

    @Override
    public Company create(String companyName, String uniqueKey) {
        return companyRepo.save(new CompanyNode(companyName, uniqueKey));
    }

}
