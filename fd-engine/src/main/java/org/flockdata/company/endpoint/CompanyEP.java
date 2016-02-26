/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
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
@RequestMapping("${org.fd.engine.system.api:api}/v1/company")
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