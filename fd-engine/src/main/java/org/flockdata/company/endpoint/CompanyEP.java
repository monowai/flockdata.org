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

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import org.flockdata.data.Company;
import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.engine.data.graph.CompanyNode;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.services.CompanyService;
import org.flockdata.track.bean.DocumentResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mholdsworth
 * @tag Endpoint, Company
 * @since 4/05/2013
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/company")
public class CompanyEP {

  private static final Logger logger = LoggerFactory
      .getLogger(CompanyEP.class);

  private final CompanyService companyService;

  private final ConceptService conceptService;

  @Autowired
  public CompanyEP(CompanyService companyService, ConceptService conceptService) {
    this.companyService = companyService;
    this.conceptService = conceptService;
  }


  @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)

  public Collection<Company> findCompanies(String apiKey, @RequestHeader(value = "api-key", required = false) String apiHeaderKey) throws FlockException {
    return companyService.findCompanies(ApiKeyInterceptor.ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
  }

  @RequestMapping(value = "/{companyName}", produces = "application/json", method = RequestMethod.GET)
  public CompanyNode getCompany(@PathVariable("companyName") String companyName,
                                HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {

    CompanyNode callersCompany = CompanyResolver.resolveCompany(request);
    if (callersCompany == null) {
      throw new NotFoundException(companyName);
    }

    // ToDo Figure out what we need this to do. Currently a caller can only belong to one company
    //   so why bother letting them chose another one?
    return callersCompany;
  }


  /**
   * All documents in use by a company
   *
   * @param request used to resolve the company the logged in user represents
   * @return Documents in use by th company
   * @throws FlockException business exception
   */
  @RequestMapping(value = "/documents", method = RequestMethod.GET)
  public Collection<DocumentResultBean> getDocumentsInUse(
      HttpServletRequest request) throws FlockException {

    CompanyNode company = CompanyResolver.resolveCompany(request);
    return conceptService.getDocumentsInUse(company);

  }


}