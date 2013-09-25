/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.CompanyRepository;
import com.auditbucket.registration.repo.neo4j.CompanyUserRepository;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.registration.repo.neo4j.model.CompanyUserNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 10:05 PM
 */
@Repository
public class CompanyDaoRepo implements CompanyDao {
    private static final String FORTRESS_NAME = "fortressName";
    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    private CompanyUserRepository companyUserRepo;

    @Autowired
    Neo4jTemplate template;

    @Override
    public Company update(Company company) {
        return companyRepo.save((CompanyNode) company);
    }

    public CompanyUser save(CompanyUser companyUser) {
        return companyUserRepo.save((CompanyUserNode) companyUser);
    }


    @Override
    public Company findByPropertyValue(String name, Object value) {
        return companyRepo.findByPropertyValue(name, value);
    }

    @Override
    public CompanyUser getCompanyUser(Long id, String userName) {
        return companyRepo.getCompanyUser(id, userName);
    }

    @Override
    public Fortress getFortressByName(Long companyId, String fortressName) {
        return companyRepo.getFortressByName(companyId, fortressName);
    }

    @Override
    public Fortress getFortressByCode(Long companyId, String fortressCode) {
        return companyRepo.getFortressByCode(companyId, fortressCode);
    }


    @Override
    public SystemUser getAdminUser(Long id, String name) {
        return companyRepo.getAdminUser(id, name);
    }

    @Override
    public Iterable<CompanyUser> getCompanyUsers(String companyName) {
        return companyRepo.getCompanyUsers(companyName);
    }

    @Override
    public Company create(String companyName, String uniqueKey) {
        return companyRepo.save(new CompanyNode(companyName, uniqueKey));
    }

}
