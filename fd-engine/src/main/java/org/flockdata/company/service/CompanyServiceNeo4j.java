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

package org.flockdata.company.service;


import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.track.service.FortressService;
import org.flockdata.integration.KeyGenService;
import org.flockdata.model.Company;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.dao.CompanyDao;
import org.flockdata.registration.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@Transactional
public class CompanyServiceNeo4j implements CompanyService {

    private final CompanyDao companyDao;

    private final KeyGenService keyGenService;

    private final FortressService fortressService;

    private final SecurityHelper securityHelper;

    private static Logger logger = LoggerFactory.getLogger(CompanyServiceNeo4j.class);

    @Autowired
    public CompanyServiceNeo4j(CompanyDao companyDao, SecurityHelper securityHelper, FortressService fortressService, KeyGenService keyGenService ) {
        this.companyDao = companyDao;
        this.securityHelper = securityHelper;
        this.fortressService = fortressService;
        this.keyGenService = keyGenService;
    }

    @Override
    @Transactional
    public Company findByName(String companyName) {
        //return companyDao.findByPropertyValue("name", companyName);
        Company company=  companyDao.findByPropertyValue("name", companyName);
        logger.debug("Looking for company {}. Found {} ", companyName, company);
        return company;
    }

    @Override
    @Transactional
    public Company findByCode(String code) {
        return companyDao.findByPropertyValue("code", code);
    }

    @Transactional
    public SystemUser getAdminUser(org.flockdata.model.Company company, String name) {
        return companyDao.getAdminUser(company.getId(), name);
    }

    @Override
    public Company create(String companyName) {
        // Change to async event via spring events
        //schemaService.ensureSystemIndexes(null);
        Company company = findByName(companyName);
        if ( company == null ) {
            logger.debug("Saving company {}", companyName);
            company = new Company(companyName, keyGenService.getUniqueKey());

            return create(company);
        }
        return company;

    }

    @Transactional
    public Company create(Company company){
        company = companyDao.create(company);
        fortressService.findInternalFortress(company);// Create this at the outset
        logger.debug("Created company {}",company);
        return company;

    }

    @Override
    @Transactional
//    @Cacheable(value = "companyKeys", unless = "#result == null")
    public org.flockdata.model.Company findByApiKey(String apiKey) {
        return companyDao.findByPropertyValue("apiKey", apiKey);
    }

    @Override
    @Transactional
    public Collection<org.flockdata.model.Company> findCompanies(String userApiKey) {
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
    public Collection<org.flockdata.model.Company> findCompanies() {
        SystemUser su = securityHelper.getSysUser(true);
        if (su == null)
            return null;

        return companyDao.findCompanies(su.getId());
    }

}
