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

package com.auditbucket.registration.service;


import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auditbucket.dao.SchemaDao;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.dao.CompanyDao;
import com.auditbucket.track.model.DocumentType;

@Service
@Transactional
public class CompanyService {

    @Autowired
    private CompanyDao companyDao;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    SchemaDao schemaDao;

    @Autowired
    private SecurityHelper securityHelper;

    private static Logger logger = LoggerFactory.getLogger(CompanyService.class);

    public Company findByName(String companyName) {
        return companyDao.findByPropertyValue("name", companyName);
    }

    public Company findByCode(String code) {
        return companyDao.findByPropertyValue("code", code);
    }

    public SystemUser getAdminUser(Company company, String name) {
        return companyDao.getAdminUser(company.getId(), name);
    }


    public Company save(String companyName) {
        Company company = companyDao.create(companyName, keyGenService.getUniqueKey());
        // Change to async event via spring events
//        schemaService.ensureSystemIndexes(company);
        return company;
    }

    @Cacheable(value = "companyKeys", unless = "#result == null")
    public Company findByApiKey(String apiKey) {
        return companyDao.findByPropertyValue("apiKey", apiKey);
    }

    public Collection<Company> findCompanies(String userApiKey) {
        if ( userApiKey == null ) {
            SystemUser su = securityHelper.getSysUser(true);
            if (su !=null )
                userApiKey = su.getApiKey();
        }
        if ( userApiKey==null ){
            throw new SecurityException("Unable to resolve user API key");
        }
        return companyDao.findCompanies(userApiKey);

    }
    public Collection<Company> findCompanies() {
        SystemUser su = securityHelper.getSysUser(true);
        if (su == null)
            return null;

        return companyDao.findCompanies(su.getId());
    }


    public Collection<DocumentType> getCompanyDocumentsInUse(Company company) {
        return schemaDao.getCompanyDocumentsInUse(company);
    }


}
