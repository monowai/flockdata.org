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

import org.flockdata.authentication.SystemUserService;
import org.flockdata.company.dao.RegistrationNeo;
import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.RegistrationBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @tag Service, User, Registration, Security
 */
@Service
@Transactional
public class SystemUserServiceNeo4j implements SystemUserService {

  private final
  RegistrationNeo registrationNeo;

  @Autowired
  public SystemUserServiceNeo4j(RegistrationNeo registrationNeo) {
    this.registrationNeo = registrationNeo;
  }

  // TODO DAT-184 Gives error while enabling caching
//    @Cacheable(value = "systemUsers", unless = "#result == null")
  public SystemUser findByLogin(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Login name cannot be null");
    }
    return registrationNeo.findSysUserByName(name.toLowerCase());
  }

  public SystemUser save(Company company, RegistrationBean regBean) {
    return registrationNeo.save(company, regBean);
  }

  @Override
  public void save(SystemUser systemUser) {
    registrationNeo.save(systemUser);
  }

  public SystemUser findByApiKey(String apiKey) {
    return registrationNeo.findByApiKey(apiKey);
  }
}
