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

package org.flockdata.company.dao;

import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.SystemUserNode;
import org.flockdata.integration.KeyGenService;
import org.flockdata.registration.RegistrationBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

/**
 * @author mholdsworth
 * @tag SystemUser, Neo4j,
 * @since 20/04/2013
 */
@Repository
public class RegistrationNeo {
  private final SystemUserRepository suRepo;

  private final KeyGenService keyGenService;

  @Autowired
  public RegistrationNeo(SystemUserRepository suRepo, KeyGenService keyGenService) {
    this.suRepo = suRepo;
    this.keyGenService = keyGenService;
  }

  public SystemUserNode save(SystemUser systemUser) {
    return suRepo.save((SystemUserNode) systemUser);
  }

  @Cacheable(value = "sysUserApiKey", unless = "#result==null")
  public SystemUser findByApiKey(String apiKey) {
    if (apiKey == null) {
      return null;
    }
    return suRepo.findBySchemaPropertyValue("apiKey", apiKey);
  }

  public SystemUserNode save(Company company, RegistrationBean regBean) {
    SystemUserNode su = new SystemUserNode(regBean, company);
    su.setCompanyAccess(company);
    su.setApiKey(keyGenService.getUniqueKey());
    return save(su);
  }

  public SystemUser findSysUserByName(String name) {
    return suRepo.getSystemUser(name);
  }


}
