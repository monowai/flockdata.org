/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.engine.query.endpoint;

import org.flockdata.engine.query.service.MatrixService;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Company;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.search.model.*;
import org.flockdata.track.bean.DocumentResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Query track services
 * User: mike
 * Date: 5/04/14
 * Time: 9:31 AM
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/query")
public class QueryEP {
    @Autowired
    MatrixService matrixService;

    @Autowired
    QueryService queryService;

    @Autowired
    RegistrationService registrationService;

    @RequestMapping(value = "/matrix", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public MatrixResults getMatrixResult(@RequestBody MatrixInputBean matrixInput, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return matrixService.getMatrix(company, matrixInput);
    }


    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public EsSearchResult searchQueryParam(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.search(company, queryParams);
    }

    @RequestMapping(value = "/es", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public String searchEsParam(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException, IOException {
        Company company = CompanyResolver.resolveCompany(request);
        queryParams.setEntityOnly(false);
        queryParams.setCompany(company.getName());
        EsSearchResult result = queryService.search(company, queryParams);
        if ( result.getJson() == null )
            return "No Results found";
        return new String(result.getJson()) ;
    }

    @RequestMapping(value = "/tagcloud", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public TagCloud getTagCloudEsParam(@RequestBody TagCloudParams tagCloudParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getTagCloud(company, tagCloudParams);
    }

    @RequestMapping(value = "/key", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public EntityKeyResults getKeys(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getKeys(company, queryParams);
    }


    @RequestMapping(value = "/documents", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Collection<DocumentResultBean> getDocumentsInUse(@RequestBody (required = false) Collection<String> fortresses, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getDocumentsInUse(company, fortresses);
    }


    @RequestMapping(value = "/concepts", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public Set<DocumentResultBean> getConcepts(@RequestBody (required = false) Collection<String> documents, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getConcepts(company, documents);
    }


    @RequestMapping(value = "/relationships", method = RequestMethod.POST,consumes = "application/json", produces = "application/json")
    public Set<DocumentResultBean> getRelationships(@RequestBody(required = false) Collection<String> documents, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        // Todo: DAT-100 Sherry's comment. Should be Concepts, not Doc Types
        return queryService.getConcepts(company, documents, true);
    }

}
