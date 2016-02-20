/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.company.endpoint;

import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.engine.configure.FdRestNotFoundException;
import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.engine.track.service.ConceptService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.registration.service.CompanyService;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.track.bean.DocumentResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
@RequestMapping("${fd-engine.system.api:api}/v1/company")
public class CompanyEP {

	private static final Logger logger = LoggerFactory
			.getLogger(CompanyEP.class);

    @Autowired
    CompanyService companyService;

    @Autowired
    ConceptService conceptService;

    @Autowired
    SecurityHelper securityHelper;

    @Autowired
    RegistrationService registrationService;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)

    public Collection<Company> findCompanies(String apiKey, @RequestHeader(value = "api-key", required = false) String apiHeaderKey) throws FlockException {
        return companyService.findCompanies(ApiKeyInterceptor.ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
    }

    @RequestMapping(value = "/{companyName}",  produces = "application/json", method = RequestMethod.GET)
    public Company getCompany(@PathVariable("companyName") String companyName,
                                              HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {

        Company callersCompany = CompanyResolver.resolveCompany(request);
        if ( callersCompany == null )
            throw new FdRestNotFoundException(companyName);

        // ToDo Figure out what we need this to do. Currently a caller can only belong to one company
        //   so why bother letting them chose another one?
        return callersCompany;
        //Company requestedCompany = companyService.findByName(companyName);

//        if (requestedCompany== null ) {
//            //Not Authorised
//            throw new FlockException("Company ["+companyName+"] could not be found");
//        } else {
//            return requestedCompany;
//        }
    }


    /**
     * All documents in use by a company
     */
    @RequestMapping(value = "/documents", method = RequestMethod.GET)

    public Collection<DocumentResultBean> getDocumentsInUse(
            HttpServletRequest request) throws FlockException, InterruptedException, ExecutionException, IOException {

        Company company = CompanyResolver.resolveCompany(request);
        return conceptService.getDocumentsInUse(company);

    }



}