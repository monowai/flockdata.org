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
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.CompanyUser;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.model.CompanyNode;
import com.auditbucket.registration.repo.neo4j.model.CompanyUserNode;
import com.auditbucket.registration.repo.neo4j.model.SystemUserNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    private SecurityHelper securityHelper;

    private static SystemUser GUEST = new SystemUserNode("Guest", null, null, false);

    //@Transactional
    public SystemUser registerSystemUser(RegistrationBean regBean) {
        SystemUser systemUser = systemUserService.findByName(regBean.getName());

        if (systemUser != null)
            return systemUser; // ToDo - throw RegistrationException

        Company company = companyService.findByName(regBean.getCompanyName());
        if (company == null) {
            company = companyService.save(regBean.getCompanyName());
        }
        regBean.setCompany(company);
        systemUser = systemUserService.save(regBean);


        return systemUser;
    }

    public CompanyUser addCompanyUser(String companyUser, String companyName) {
        String systemUser = securityHelper.isValidUser();
        Company company = companyService.findByName(companyName);

        if (company == null) {
            throw new IllegalArgumentException("CompanyNode does not exist");
        }

        isAdminUser(company, "[" + systemUser + "] is not authorised to add company user records for [" + companyName + "]");

        CompanyUser iCompanyUser = companyService.getCompanyUser(company, companyUser);
        if (iCompanyUser == null) {
            iCompanyUser = companyService.save(new CompanyUserNode(companyUser, company));
        }

        return iCompanyUser;
    }


    public SystemUser isAdminUser(Company company, String message) {
        String systemUser = securityHelper.isValidUser();
        SystemUser adminUser = companyService.getAdminUser(company, systemUser);
        if (adminUser == null)
            throw new IllegalArgumentException(message);
        return adminUser;
    }

    /**
     * @return currently logged-in SystemUser or Guest if anonymous
     */
    public SystemUser getSystemUser() {
        String systemUser = securityHelper.getUserName(false, false);
        if (systemUser == null)
            return GUEST;
        SystemUser iSystemUser = systemUserService.findByName(systemUser);
        if (iSystemUser == null) {
            return GUEST;
        } else {
            return iSystemUser;
        }


    }

}
