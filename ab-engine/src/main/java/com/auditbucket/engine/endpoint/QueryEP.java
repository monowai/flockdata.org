package com.auditbucket.engine.endpoint;

import com.auditbucket.engine.service.MatrixService;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.engine.service.QueryService;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.query.MatrixInputBean;
import com.auditbucket.query.MatrixResult;
import com.auditbucket.query.MatrixResults;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

    /**
     * @param metaHeader
     * @param apiKey
     * @param apiHeaderKey
     * @return
     * @throws DatagioException
     * @deprecated use POST version
     */
    @ResponseBody
    @RequestMapping(value = "/matrix/{metaHeader}", method = RequestMethod.GET)
    public Collection<MatrixResult> getMatrix(@PathVariable("metaHeader") String metaHeader,
                                              String apiKey,
                                              @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        if (metaHeader == null || metaHeader.equalsIgnoreCase("_all")) {
            metaHeader = null;
        }
        ArrayList<String> labels = new ArrayList<>();
        labels.add(metaHeader);
        MatrixInputBean input = new MatrixInputBean();
        input.setDocuments(labels);

        ArrayList<String> from = new ArrayList<>();
        from.add("writer");
        from.add("lead");
        input.setFromRlxs(from);
        ArrayList<String> to = new ArrayList<>();

        to.add("writer");
        to.add("lead");
        to.add("contributor");
        input.setToRlxs(to);
        ArrayList<String> tags = new ArrayList<>();
        tags.add("Person");
        input.setConcepts(tags);
        input.setMinCount(2);
        return getMatrixResult(input, apiKey, apiHeaderKey).getResults();
    }

    @ResponseBody
    @RequestMapping(value = "/matrix/", method = RequestMethod.POST)
    public MatrixResults getMatrixResult(@PathVariable("metaHeader") MatrixInputBean matrixInput,
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

    public Collection<DocumentType> getDocumentsInUse(Collection<String> fortresses, String apiKey,
                                                @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {

        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return queryService.getDocumentsInUse(abCompany, fortresses);
    }

    public Set<TrackTag> getTags(Collection<String> documents, String apiKey,
                                 @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return queryService.getTags(abCompany, documents);
    }

    public Collection<String> getRelationships(Collection<String> concepts, String apiKey,
                                               @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));

        return queryService.getRelationships(abCompany, concepts);
    }


}
