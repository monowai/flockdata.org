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

import com.auditbucket.registration.dao.CompanyDao;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.CompanyRepository;
import com.auditbucket.registration.repo.neo4j.CompanyUserRepository;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.registration.repo.neo4j.model.CompanyUserNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 10:05 PM
 */
@Repository
public class CompanyDaoRepo implements CompanyDao {
    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    private CompanyUserRepository companyUserRepo;


    @Override
    public Company save(Company company) {
        return companyRepo.save((CompanyNode) company);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CompanyUser save(CompanyUser companyUser) {
        return companyUserRepo.save((CompanyUserNode) companyUser);  //To change body of implemented methods use File | Settings | File Templates.
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
    public Fortress getFortress(Long companyId, String fortressName) {
        return companyRepo.getFortress(companyId, fortressName);
    }

    @Override
    public SystemUser getAdminUser(Long id, String name) {
        return companyRepo.getAdminUser(id, name);
    }

    @Override
    public Iterable<CompanyUser> getCompanyUsers(String companyName) {
        return companyRepo.getCompanyUsers(companyName);
    }
}
