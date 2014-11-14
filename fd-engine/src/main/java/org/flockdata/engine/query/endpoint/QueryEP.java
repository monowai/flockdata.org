/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

package org.flockdata.engine.query.endpoint;

import org.flockdata.engine.query.service.MatrixService;
import org.flockdata.engine.query.service.QueryService;
import org.flockdata.helper.CompanyResolver;
import org.flockdata.helper.FlockException;
import org.flockdata.query.MatrixInputBean;
import org.flockdata.query.MatrixResults;
import org.flockdata.registration.model.Company;
import org.flockdata.registration.service.RegistrationService;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.search.model.TagCloud;
import org.flockdata.search.model.TagCloudParams;
import org.flockdata.track.bean.DocumentResultBean;
import org.flockdata.track.service.MediationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Set;

/**
 * Query track services
 * User: mike
 * Date: 5/04/14
 * Time: 9:31 AM
 */
@RestController
@RequestMapping("/query")
public class QueryEP {
    @Autowired
    MatrixService matrixService;

    @Autowired
    QueryService queryService;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    MediationFacade mediationFacade;


    @RequestMapping(value = "/matrix", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public MatrixResults getMatrixResult(@RequestBody MatrixInputBean matrixInput, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return matrixService.getMatrix(company, matrixInput);
    }


    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public EsSearchResult searchQueryParam(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.search(company, queryParams);
    }

    @RequestMapping(value = "/es", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public EsSearchResult searchEsParam(@RequestBody QueryParams queryParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        queryParams.setEntityOnly(false);
        return mediationFacade.search(company, queryParams);
    }

    @RequestMapping(value = "/tagcloud", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public TagCloud getTagCloudEsParam(@RequestBody TagCloudParams tagCloudParams, HttpServletRequest request) throws FlockException {
        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.getTagCloud(company, tagCloudParams);
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
