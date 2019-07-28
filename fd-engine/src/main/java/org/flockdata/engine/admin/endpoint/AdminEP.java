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

package org.flockdata.engine.admin.endpoint;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.authentication.SecurityHelper;
import org.flockdata.engine.admin.AdminResponse;
import org.flockdata.engine.admin.PlatformConfig;
import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.tag.MediationFacade;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Engine admin
 *
 * @author mholdsworth
 * @tag EndPoint, Fortress
 * @since 15/04/2014
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/admin")
public class AdminEP {

  private static Logger logger = LoggerFactory.getLogger(AdminEP.class);
  private final MediationFacade mediationFacade;
  private final SecurityHelper securityHelper;
  private final PlatformConfig engineConfig;

  @Autowired
  public AdminEP(@Qualifier("mediationFacadeNeo") MediationFacade mediationFacade, SecurityHelper securityHelper, PlatformConfig engineConfig) {
    this.mediationFacade = mediationFacade;
    this.securityHelper = securityHelper;
    this.engineConfig = engineConfig;
  }

  @RequestMapping(value = "/ping", method = RequestMethod.GET)
  public String getPing() {
    // curl -X GET http://localhost:8081/api/v1/track/ping
    return engineConfig.authPing();
  }


  @RequestMapping(value = "/health", method = RequestMethod.GET)
  public Map<String, Object> getHealth(HttpServletRequest request) throws FlockException {
    Object o = request.getAttribute(ApiKeyInterceptor.API_KEY);
    if (o == null) {
      o = request.getHeader(ApiKeyInterceptor.API_KEY);
    }
    String apiKey = "";
    if (o != null) {
      apiKey = o.toString();
    }

    if ("".equals(apiKey)) {
      apiKey = null;
    }
    if (request.getAttribute(ApiKeyInterceptor.COMPANY) == null &&
        apiKey == null) {
      return engineConfig.getHealthAuth();// Caller may have admin role but not belong to a company
    }
    return engineConfig.getHealth();
  }


  @RequestMapping(value = "/{fortressCode:.*}/rebuild", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.ACCEPTED)
  public AdminResponse rebuildSearch(@PathVariable("fortressCode") String fortressCode,
                                     HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    logger.info("Reindex command received for " + fortressCode + " from [" + securityHelper.getLoggedInUser() + "]");
    String message = mediationFacade.reindex(company, fortressCode);
    return new AdminResponse(message);
  }


  @RequestMapping(value = "/{fortressName:.*}/{docType}/rebuild", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.ACCEPTED)
  public AdminResponse rebuildSearch(@PathVariable("fortressName") String fortressName, @PathVariable("docType") String docType,
                                     HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    logger.info("Reindex command received for " + fortressName + " & docType " + docType + " from [" + securityHelper.getLoggedInUser() + "]");
    String message = mediationFacade.reindexByDocType(company, fortressName, docType);
    return new AdminResponse(message);
  }

  @RequestMapping(value = "/{code:.*}/{docType}", method = RequestMethod.DELETE)
  public AdminResponse deleteDocType(@PathVariable("code") String fortressCode, @PathVariable("docType") String docType, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    mediationFacade.purge(company, fortressCode, docType);
    return new AdminResponse("Purging " + fortressCode + "... This may take a while");
  }

  @RequestMapping(value = "/{fortressName:.*}", method = RequestMethod.DELETE)
  @ResponseStatus(value = HttpStatus.ACCEPTED)
  public AdminResponse purgeFortress(@PathVariable("fortressName") String fortressCode,
                                     HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    mediationFacade.purge(company, fortressCode);
    return new AdminResponse("Purging " + fortressCode + "... This may take a while");

  }


  @RequestMapping(value = "/{code:.*}/{docType}/{segment}", method = RequestMethod.DELETE)
  public AdminResponse deleteDocType(@PathVariable("code") String fortressCode,
                                     @PathVariable("docType") String docType,
                                     @PathVariable("segment") String segment, HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);
    mediationFacade.purge(company, fortressCode, docType, segment);
    return new AdminResponse("Purging " + fortressCode + "... This may take a while");
  }


  @RequestMapping(value = "/{fortressName:.*}/{docType}/validate", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.ACCEPTED)
  public AdminResponse validateFromSearch(@PathVariable("fortressName") String fortressName, @PathVariable("docType") String docType,
                                          HttpServletRequest request) throws FlockException {
    CompanyNode company = CompanyResolver.resolveCompany(request);

    logger.info("Validate command received for " + fortressName + " & docType " + docType + " from [" + securityHelper.getLoggedInUser() + "]");
    String message = mediationFacade.validateFromSearch(company, fortressName, docType);

    return new AdminResponse(message);
  }

}
