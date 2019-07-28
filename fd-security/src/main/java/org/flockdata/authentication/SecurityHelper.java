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

package org.flockdata.authentication;

import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author mholdsworth
 * @tag Helper, Security, APIKey
 * @since 17/04/2013
 */
@Service
public class SecurityHelper {

  private final SystemUserService sysUserService;

  @Autowired
  public SecurityHelper(SystemUserService sysUserService) {
    this.sysUserService = sysUserService;
  }

  public String getLoggedInUser() {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) {
      throw new SecurityException("User is not authenticated");
    }
    return a.getName();
  }

  public String getUserName(boolean exceptionOnNull, boolean isSysUser) {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) {
      if (exceptionOnNull) {
        throw new SecurityException("User is not authenticated");
      } else {
        return null;
      }
    }

    if (isSysUser) {
      SystemUser su = getSysUser(a.getName());
      if (su == null) {
        throw new IllegalArgumentException("Not authorised");
      }
    }
    return a.getName();
  }

  public SystemUser getSysUser(boolean exceptionOnNull) {
    Authentication a = SecurityContextHolder.getContext().getAuthentication();

    if (a == null) {
      if (exceptionOnNull) {
        throw new SecurityException("User is not authenticated");
      } else {
        return null;
      }
    }

    return sysUserService.findByLogin(a.getName());
  }

  SystemUser getSysUser(String loginName) {
    return sysUserService.findByLogin(loginName);
  }

  public Company getCompany() {
    String userName = getLoggedInUser();
    SystemUser su = sysUserService.findByLogin(userName);

    if (su == null) {
      throw new SecurityException("Not authorised");
    }

    return su.getCompany();
  }

  //    @Cacheable(value = "company", unless = "#result == null")
  public Company getCompany(String usersApiKey) {
    if (usersApiKey == null) {
      return getCompany();
    }

    SystemUser su = sysUserService.findByApiKey(usersApiKey);
    if (su == null) {
      return null;
    }
    return su.getCompany();
  }
}
