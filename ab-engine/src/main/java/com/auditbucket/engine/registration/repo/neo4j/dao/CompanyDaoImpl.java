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

package com.auditbucket.engine.registration.repo.neo4j.dao;

import com.auditbucket.engine.registration.dao.CompanyDaoI;
import com.auditbucket.engine.registration.repo.neo4j.CompanyRepository;
import com.auditbucket.engine.registration.repo.neo4j.CompanyUserRepository;
import com.auditbucket.engine.registration.repo.neo4j.model.Company;
import com.auditbucket.engine.registration.repo.neo4j.model.CompanyUser;
import com.auditbucket.registration.model.ICompany;
import com.auditbucket.registration.model.ICompanyUser;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.ISystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * User: Mike Holdsworth
 * Date: 20/04/13
 * Time: 10:05 PM
 */
@Repository
public class CompanyDaoImpl implements CompanyDaoI {
    @Autowired
    private CompanyRepository companyRepo;

    @Autowired
    private CompanyUserRepository companyUserRepo;


    @Override
    public ICompany save(ICompany company) {
        return companyRepo.save((Company) company);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ICompanyUser save(ICompanyUser companyUser) {
        return companyUserRepo.save((CompanyUser) companyUser);  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public ICompany findByPropertyValue(String name, Object value) {
        return companyRepo.findByPropertyValue(name, value);
    }

    @Override
    public ICompanyUser getCompanyUser(Long id, String userName) {
        return companyRepo.getCompanyUser(id, userName);
    }

    @Override
    public IFortress getFortress(Long companyId, String fortressName) {
        return companyRepo.getFortress(companyId, fortressName);
    }

    @Override
    public ISystemUser getAdminUser(Long id, String name) {
        return companyRepo.getAdminUser(id, name);
    }

    @Override
    public Iterable<ICompanyUser> getCompanyUsers(String companyName) {
        return companyRepo.getCompanyUsers(companyName);
    }
}
