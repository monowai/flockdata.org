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

package com.auditbucket.registration;


import com.auditbucket.engine.repo.neo4j.dao.SchemaDaoNeo4j;
import com.auditbucket.engine.service.EngineConfig;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.dao.CompanyDao;
import com.auditbucket.registration.dao.neo4j.model.CompanyNode;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.KeyGenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@Transactional
public class CompanyServiceNeo4j implements CompanyService {

    @Autowired
    private CompanyDao companyDao;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    EngineConfig engineConfig;

    @Autowired
    SchemaDaoNeo4j schemaDao;

    @Autowired
    com.auditbucket.track.service.SchemaService schemaService;

    @Autowired
    private SecurityHelper securityHelper;

    private static Logger logger = LoggerFactory.getLogger(CompanyServiceNeo4j.class);

    @Override
    @Transactional
    public Company findByName(String companyName) {
        return companyDao.findByPropertyValue("name", companyName);
    }

    @Override
    @Transactional
    public Company findByCode(String code) {
        return companyDao.findByPropertyValue("code", code);
    }

    @Override
    @Transactional
    public SystemUser getAdminUser(Company company, String name) {
        return companyDao.getAdminUser(company.getId(), name);
    }

    @Override
    public Company save(String companyName) {
        // Change to async event via spring events
        logger.debug("Saving company {}",companyName);
        Company company = new CompanyNode(companyName, keyGenService.getUniqueKey());
        Future<Boolean> worked = schemaService.ensureSystemIndexes(company);
        try {
            logger.debug("Waiting for system indexes to finish");
            worked.get();
            Thread.sleep(100);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unexpected", e);
        }
        return create(company);

    }
    @Transactional
    public Company create(Company company){
        company = companyDao.create(company);
        logger.debug("Created company {}",company);
        return company;

    }

    @Override
    @Transactional
//    @Cacheable(value = "companyKeys", unless = "#result == null")
    public Company findByApiKey(String apiKey) {
        return companyDao.findByPropertyValue("apiKey", apiKey);
    }

    @Override
    @Transactional
    public Collection<Company> findCompanies(String userApiKey) {
        if (userApiKey == null) {
            SystemUser su = securityHelper.getSysUser(true);
            if (su != null)
                userApiKey = su.getApiKey();
        }
        if (userApiKey == null) {
            throw new SecurityException("Unable to resolve user API key");
        }
        return companyDao.findCompanies(userApiKey);

    }

    @Override
    @Transactional
    public Collection<Company> findCompanies() {
        SystemUser su = securityHelper.getSysUser(true);
        if (su == null)
            return null;

        return companyDao.findCompanies(su.getId());
    }

}
