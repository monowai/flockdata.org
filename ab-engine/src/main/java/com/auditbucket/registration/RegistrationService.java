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

import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.repo.neo4j.model.SystemUserNode;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.KeyGenService;
import com.auditbucket.registration.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegistrationService implements com.auditbucket.registration.service.RegistrationService {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    private SecurityHelper securityHelper;

    public static SystemUser GUEST = new SystemUserNode("Guest", null, null, false);


    @Override
    @Secured({"ROLE_AB_ADMIN"})
    public SystemUser registerSystemUser(RegistrationBean regBean) throws DatagioException {

        SystemUser systemUser = systemUserService.findByLogin(regBean.getLogin());

        if (systemUser != null) {
        	return systemUser; 
        }

        Company company = companyService.findByName(regBean.getCompanyName());
        if (company == null) {
            company = companyService.save(regBean.getCompanyName());
        }
        regBean.setCompany(company);
        systemUser = systemUserService.save(regBean);

        return systemUser;
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
        SystemUser iSystemUser = systemUserService.findByLogin(systemUser);
        if (iSystemUser == null ) {
            // Authenticated in the security system, but not in the graph
            return new SystemUserNode(systemUser, null, null, true);
        } else {
            return iSystemUser;
        }
    }

    public SystemUser getSystemUser(String apiKey){
        SystemUser su = systemUserService.findByApiKey(apiKey);
        if ( su == null )
            return getSystemUser();
        return su;
    }

    public Company resolveCompany(String apiKey) throws DatagioException {
        Company c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new DatagioException("Invalid API Key");
        return c;
    }
}
