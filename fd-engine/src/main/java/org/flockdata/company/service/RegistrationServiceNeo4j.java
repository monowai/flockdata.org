/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
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

import org.flockdata.authentication.FdRoles;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.authentication.SystemUserService;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.data.graph.SystemUserNode;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.KeyGenService;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.services.CompanyService;
import org.flockdata.services.RegistrationService;
import org.flockdata.services.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @tag Service, Company, Registration, Security
 */
@Service
public class RegistrationServiceNeo4j implements RegistrationService {

  public static SystemUserNode GUEST = new SystemUserNode("Guest", null, null, false);
  private final CompanyService companyService;
  private final SystemUserService systemUserService;
  private final KeyGenService keyGenService;
  private final SecurityHelper securityHelper;
  private Logger logger = LoggerFactory.getLogger(RegistrationServiceNeo4j.class);

  @Autowired
  public RegistrationServiceNeo4j(CompanyService companyService, SystemUserService systemUserService, KeyGenService keyGenService, SchemaService schemaService, @Qualifier("engineConfig") PlatformConfig engineConfig, SecurityHelper securityHelper) {
    this.companyService = companyService;
    this.systemUserService = systemUserService;
    this.keyGenService = keyGenService;
    this.securityHelper = securityHelper;
  }

  @Override
  @Transactional
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public SystemUser registerSystemUser(Company company, RegistrationBean regBean) throws FlockException {

    SystemUserNode systemUser = (SystemUserNode) systemUserService.findByLogin(regBean.getLogin());

    if (systemUser != null) {
      if (systemUser.getApiKey() == null) {
        systemUser.setApiKey(keyGenService.getUniqueKey());
        systemUserService.save(systemUser);
      }
      logger.debug("Returning existing SU {}", systemUser);
      return systemUser;
    }

    return makeSystemUser(company, regBean);
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public SystemUser registerSystemUser(RegistrationBean regBean) throws FlockException {
    // Non-transactional method

//        if (engineConfig.createSystemConstraints())
//            schemaService.ensureSystemIndexes(null);

    Company company = companyService.findByName(regBean.getCompanyName());
    if (company == null) {
      company = companyService.create(regBean.getCompanyName());

    }

    return registerSystemUser(company, regBean);
  }

  @Transactional
  public SystemUser makeSystemUser(Company company, RegistrationBean regBean) {
    logger.debug("Creating new system user {}", regBean);
    return systemUserService.save(company, regBean);


  }

  /**
   * @return currently logged-in SystemUser or Guest if anonymous
   */
  @Transactional
  public SystemUser getSystemUser() {
    String systemUser = securityHelper.getUserName(false, false);
    if (systemUser == null) {
      return GUEST;
    }
    SystemUser iSystemUser = systemUserService.findByLogin(systemUser);
    if (iSystemUser == null) {
      // Authenticated in the security system, but not in the graph
      return new SystemUserNode(systemUser, null, null, true);
    } else {
      return iSystemUser;
    }
  }

  @Transactional
  public SystemUser getSystemUser(String apiKey) {
      if (apiKey == null) {
          return GUEST;
      }
    SystemUser su = systemUserService.findByApiKey(apiKey);
    if (su == null) {
      return getSystemUser();
    }
    return su;
  }

  public Company resolveCompany(String apiKey) throws FlockException {
    Company c = securityHelper.getCompany(apiKey);
    if (c == null) {
      throw new FlockException("Invalid API Key");
    }
    return c;
  }
}
