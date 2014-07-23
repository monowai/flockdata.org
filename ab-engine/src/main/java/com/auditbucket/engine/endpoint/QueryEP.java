package com.auditbucket.engine.endpoint;

import com.auditbucket.engine.service.MatrixService;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.QueryService;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.MetaHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    @RequestMapping(value = "/matrix/", method = RequestMethod.POST)
    public MatrixResults getMatrixResult(@RequestBody MatrixInputBean matrixInput,
                                         String apiKey,
                                         @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return matrixService.getMatrix(company, matrixInput);
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public EsSearchResult<Collection<MetaHeader>> searchQueryParam(@RequestBody QueryParams queryParams,
                                                                   String apiKey,
                                                                   @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {

        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        queryParams.setCompany(abCompany.getName());
        return mediationFacade.search(abCompany, queryParams);
    }

    @ResponseBody
    @RequestMapping(value = "/documents/", method = RequestMethod.POST)
    public Collection<DocumentType> getDocumentsInUse(@RequestBody (required = false) Collection<String> fortresses, String apiKey,
                                                @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {

        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return queryService.getDocumentsInUse(abCompany, fortresses);
    }

    @ResponseBody
    @RequestMapping(value = "/concepts/", method = RequestMethod.POST)
    public Set<DocumentType> getConcepts(@RequestBody (required = false) Collection<String> documents, String apiKey,
                                    @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return queryService.getConcepts(abCompany, documents);
    }

    @ResponseBody
    @RequestMapping(value = "/relationships/", method = RequestMethod.POST)
    public Set<DocumentType> getRelationships(@RequestBody(required = false) Collection<String> documents, String apiKey,
                                               @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        // Todo: DAT-100 Sherry's comment. Should be Concepts, not Doc Types
        return queryService.getConcepts(abCompany, documents, true);
    }


}
