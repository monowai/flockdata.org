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

package org.flockdata.company.endpoint;

import org.flockdata.authentication.UserProfileService;
import org.flockdata.data.SystemUser;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.services.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Endpoint, Registration, SystemUser
 * @since 4/05/2013
 */
@RestController
// Customise a dispatcher in web.xml
@RequestMapping("${org.fd.engine.system.api:api}/v1/profiles")
public class RegistrationEP {

  private final RegistrationService regService;

  private final UserProfileService userProfileService;

  @Autowired
  public RegistrationEP(UserProfileService userProfileService, RegistrationService regService) {
    this.userProfileService = userProfileService;
    this.regService = regService;
  }

  @RequestMapping(value = "/", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.CREATED)
  public SystemUserResultBean registerSystemUser(@RequestBody RegistrationBean regBean) throws FlockException {
    // curl -u admin:hackme -H "Content-Type:application/json" -X PUT http://localhost:8080/api/v1/profiles -d '{"name":"mikey", "companyName":"Monowai Dev","password":"whocares"}'
    SystemUser su = regService.registerSystemUser(regBean);

    if (su == null) {
      return new SystemUserResultBean(su);
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return new SystemUserResultBean(su, userProfileService.getUser(auth));
  }

  @RequestMapping(value = "/me", method = RequestMethod.GET, produces = "application/json")
  public SystemUserResultBean get(@RequestHeader(value = "api-key",
      required = false) String apiHeaderKey) {
    // curl -u batch:123 -X GET http://localhost:8080/ab/profiles/me/
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    SystemUserResultBean su = new SystemUserResultBean(regService.getSystemUser(apiHeaderKey), userProfileService.getUser(auth));
    return su;

  }


}