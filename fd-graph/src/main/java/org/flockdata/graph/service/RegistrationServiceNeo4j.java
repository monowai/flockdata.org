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

package org.flockdata.graph.service;

import org.flockdata.authentication.FdRoles;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.authentication.SystemUserService;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.graph.dao.CompanyRepo;
import org.flockdata.graph.model.CompanyNode;
import org.flockdata.graph.model.SystemUserNode;
import org.flockdata.helper.FlockException;
import org.flockdata.integration.KeyGenService;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.services.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @tag Service, Company, Registration, Security
 */
@Service
public class RegistrationServiceNeo4j implements RegistrationService {

  public static SystemUserNode GUEST = SystemUserNode.builder().login("Guest").build();
  private final CompanyRepo companyRepo;
  private final SystemUserService systemUserService;
  private final KeyGenService keyGenService;
  private final SecurityHelper securityHelper;
  private Logger logger = LoggerFactory.getLogger(RegistrationServiceNeo4j.class);

  @Autowired
  public RegistrationServiceNeo4j(CompanyRepo companyRepo, SystemUserService systemUserService, KeyGenService keyGenService, SecurityHelper securityHelper) {
    this.companyRepo = companyRepo;
    this.systemUserService = systemUserService;
    this.keyGenService = keyGenService;
    this.securityHelper = securityHelper;
  }

  @Override
//    @Transactional
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

    regBean.setCompany(company);
    return makeSystemUser(company, regBean);
  }

  @Override
  @PreAuthorize(FdRoles.EXP_ADMIN)
  public SystemUser registerSystemUser(RegistrationBean regBean) throws FlockException {
    Company company = regBean.getCompany();
    if (company == null && regBean.getCompanyName() != null) {
      // Might be a new Company
      company = CompanyNode.builder()
          .name(regBean.getCompanyName())
          .code(regBean.getCompanyName().toLowerCase())
          .apiKey(keyGenService.getUniqueKey())
          .build();
      regBean.setCompany(company);
    }

    if (company != null && company.getId() == null) {
      company = companyRepo.findByCode(company);
    }

    if (company == null) {
      company = companyRepo.create(regBean.getCompany());
    }


    return registerSystemUser(company, regBean);
  }

  //    @Transactional
  public SystemUser makeSystemUser(Company company, RegistrationBean regBean) {
    logger.debug("Creating new system user {}", regBean);
    return systemUserService.save(company, regBean);


  }

  /**
   * @return currently logged-in SystemUser or Guest if anonymous
   */
//    @Transactional
  public SystemUser getSystemUser() {
    String systemUser = securityHelper.getUserName(false, false);
    if (systemUser == null) {
      return GUEST;
    }
    SystemUser iSystemUser = systemUserService.findByLogin(systemUser);
    if (iSystemUser == null) {
      // Authenticated in the security system, but not in the graph
      return SystemUserNode.builder().login(systemUser).build();
    } else {
      return iSystemUser;
    }
  }

  //    @Transactional
  public SystemUser getSystemUser(String apiKey) {
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
