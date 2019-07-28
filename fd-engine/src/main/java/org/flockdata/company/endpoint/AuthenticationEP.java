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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.flockdata.authentication.UserProfileService;
import org.flockdata.registration.LoginRequest;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.services.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @tag Endpoint, Security
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/")
public class AuthenticationEP {

  //private static final Logger logger = LoggerFactory.getLogger(AuthenticationEP.class);

  private final AuthenticationManager authenticationManager;

  private final RegistrationService regService;


  private final UserProfileService userProfileService;

  @Autowired(required = false)
  public AuthenticationEP(@Qualifier("authenticationManager") AuthenticationManager authenticationManager, RegistrationService regService, UserProfileService userProfileService) {
    this.authenticationManager = authenticationManager;
    this.regService = regService;
    this.userProfileService = userProfileService;
  }

  @RequestMapping(value = "/ping", method = RequestMethod.GET)
  public String getPing() {
    // curl -X GET http://localhost:8081/api/ping
    return "pong";
  }


  @RequestMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
  public ResponseEntity<SystemUserResultBean> handleLogin(@RequestBody LoginRequest loginRequest) throws Exception {
    String username = loginRequest.getUsername();
    String password = loginRequest.getPassword();

    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
        username, password);
    Authentication auth = authenticationManager.authenticate(token);
    SecurityContextHolder.getContext().setAuthentication(auth);
    return new ResponseEntity<>(new SystemUserResultBean(regService.getSystemUser(), userProfileService.getUser(auth)), HttpStatus.OK);
  }

  /**
   * GET  /account returns current logged in user
   *
   * @param apiHeaderKey optional, user can be located via an apiKey if you have one
   * @return view of user associated with the key
   * @throws Exception error
   */
  @RequestMapping(value = "/account", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
  public ResponseEntity<SystemUserResultBean> checkUser(@RequestHeader(value = "api-key", required = false) String apiHeaderKey) throws Exception {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth instanceof AnonymousAuthenticationToken) {

      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    return new ResponseEntity<>(new SystemUserResultBean(regService.getSystemUser(apiHeaderKey), userProfileService.getUser(auth)), HttpStatus.OK);
  }

  /**
   * GET  /logout logout the currently logged in user.
   *
   * @param request  servlet request context
   * @param response servlet response
   */
  @RequestMapping(value = "/logout", method = RequestMethod.GET)
  public void handleLogout(HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      new SecurityContextLogoutHandler().logout(request, response, auth);
    }
    SecurityContextHolder.getContext().setAuthentication(null);
  }
}
