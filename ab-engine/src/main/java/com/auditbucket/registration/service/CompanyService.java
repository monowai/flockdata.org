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


import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.dao.CompanyDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class CompanyService {


    @Autowired
    private CompanyDao companyDao;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private TagService tagService;


    public Company findByName(String companyName) {
        return companyDao.findByPropertyValue("name", companyName);
    }

    public Company findByCode(String code) {
        return companyDao.findByPropertyValue("code", code);
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

    @Cacheable(value = "companyFortress", unless = "#result == null")
    public Fortress getCompanyFortress(Long company, String fortressName) {
        return companyDao.getFortressByName(company, fortressName);
    }

    public Iterable<CompanyUser> getUsers(String companyName) {
        return companyDao.getCompanyUsers(companyName);
    }


    public Fortress getFortress(Company company, String name) {

        return companyDao.getFortressByName(company.getId(), name);
    }

    public SystemUser getAdminUser(Company company, String name) {
        return companyDao.getAdminUser(company.getId(), name);
    }


    public CompanyUser save(CompanyUser companyUser) {
        return companyDao.save(companyUser);
    }

    public Company save(String companyName) {
        Company company = companyDao.create(companyName, keyGenService.getUniqueKey());
        tagService.createCompanyTagManager(company.getId(), companyName + "Tags");
        return company;
    }

    @Cacheable(value = "companyKeys", unless = "#result == null")
    public Company findByApiKey(String apiKey) {
        return companyDao.findByPropertyValue("apiKey", apiKey);
    }


    public Collection<Company> findCompanies() {
        SystemUser su = securityHelper.getSysUser(true);
        if (su == null)
            return null;

        return companyDao.findCompanies(su.getId());
    }

}
