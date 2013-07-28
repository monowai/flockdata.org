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

package com.auditbucket.registration.service;


import com.auditbucket.registration.dao.CompanyDaoI;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {


    @Autowired
    private CompanyDaoI companyDao;


    public Company findByName(String companyName) {
        return companyDao.findByPropertyValue("name", companyName);
    }

    public CompanyUser getCompanyUser(Company company, String userName) {
        return companyDao.getCompanyUser(company.getId(), userName);

    }

    public CompanyUser getCompanyUser(String companyName, String userName) {
        Company company = findByName(companyName);
        if (company == null)
            return null;
        return companyDao.getCompanyUser(company.getId(), userName);

    }

    public Fortress getCompanyFortress(Company company, String fortressName) {
        return companyDao.getFortress(company.getId(), fortressName);
    }


    public Company save(Company company) {
        return companyDao.save(company);
    }


    public Iterable<CompanyUser> getUsers(String companyName) {
        return companyDao.getCompanyUsers(companyName);
    }


    public Fortress getFortress(Company company, String name) {
        return companyDao.getFortress(company.getId(), name);
    }

    public SystemUser getAdminUser(Company company, String name) {
        return companyDao.getAdminUser(company.getId(), name);
    }


    public CompanyUser save(CompanyUser companyUser) {
        return companyDao.save(companyUser);
    }

}
