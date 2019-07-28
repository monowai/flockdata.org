/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.authentication.simple;

import org.flockdata.authentication.SystemUserService;
import org.flockdata.authentication.UserProfileService;
import org.flockdata.data.SystemUser;
import org.flockdata.registration.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

/**
 * Encapsulates a user account mastered in a configuration file
 *
 * @tag SystemUser, Security
 */
@Component
public class SimpleUser implements UserProfileService {

  private static Logger logger = LoggerFactory.getLogger("configuration");
  @Autowired(required = false)
  private SystemUserService systemUserService = null;

  public UserProfile getUser(Authentication authentication) {
    Object userName = authentication.getPrincipal();
    String login;
    User auth = null;
    if (userName instanceof String) {
      login = (String) userName;
    } else {
      login = ((User) authentication.getPrincipal()).getUsername();
      auth = (User) authentication.getPrincipal();
    }

    UserProfile userProfile = new UserProfile();
    userProfile.setUserId(login);
    userProfile.setStatus("ENABLED");

    if (auth != null && !auth.getAuthorities().isEmpty()) {
      for (GrantedAuthority grantedAuthority : auth.getAuthorities()) {
        userProfile.addUserRole(grantedAuthority.getAuthority());
      }
    }
    if (auth != null && systemUserService != null) {
      SystemUser sysUser = systemUserService.findByLogin(login);
      if (sysUser != null) {
        userProfile.setApiKey(sysUser.getApiKey());
        userProfile.setCompany(sysUser.getCompany().getName());
      }
    }

    return userProfile;
  }

  public String getProvider() {
    return "Simple SimpleUser-Security";
  }

}
