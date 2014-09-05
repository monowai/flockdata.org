package com.auditbucket.engine.endpoint;

import com.auditbucket.engine.service.MatrixService;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.QueryService;
import com.auditbucket.helper.CompanyResolver;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.bean.DocumentResultBean;
import com.auditbucket.track.model.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Set;

/**
 * Query track services
 * User: mike
 * Date: 5/04/14
 * Time: 9:31 AM
 */
@Controller
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

    @ResponseBody
    @RequestMapping(value = "/matrix", method = RequestMethod.POST)
    public MatrixResults getMatrixResult(@RequestBody MatrixInputBean matrixInput, HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return matrixService.getMatrix(company, matrixInput);
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public EsSearchResult searchQueryParam(@RequestBody QueryParams queryParams, HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return mediationFacade.search(company, queryParams);
    }

    @ResponseBody
    @RequestMapping(value = "/documents", method = RequestMethod.POST)
    public Collection<DocumentResultBean> getDocumentsInUse(@RequestBody (required = false) Collection<String> fortresses, HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getDocumentsInUse(company, fortresses);
    }

    @ResponseBody
    @RequestMapping(value = "/concepts", method = RequestMethod.POST)
    public Set<DocumentResultBean> getConcepts(@RequestBody (required = false) Collection<String> documents, HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        return queryService.getConcepts(company, documents);
    }

    @ResponseBody
    @RequestMapping(value = "/relationships", method = RequestMethod.POST)
    public Set<DocumentResultBean> getRelationships(@RequestBody(required = false) Collection<String> documents, HttpServletRequest request) throws DatagioException {
        Company company = CompanyResolver.resolveCompany(request);
        // Todo: DAT-100 Sherry's comment. Should be Concepts, not Doc Types
        return queryService.getConcepts(company, documents, true);
    }

}
