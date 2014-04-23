package com.auditbucket.engine.endpoint;

import com.auditbucket.audit.model.MetaHeader;
import com.auditbucket.engine.service.MatrixService;
import com.auditbucket.engine.service.MediationFacade;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.query.MatrixResult;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.model.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import javax.ws.rs.QueryParam;
import java.util.Collection;
import java.util.Map;

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
    MatrixService service;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    MediationFacade mediationFacade;

    @ResponseBody
    @RequestMapping(value = "/matrix/{metaHeader}", method = RequestMethod.GET)
    public Map<String, Map<String, Long>> getMatrix(@PathVariable("metaHeader") String metaHeader,
                                                    String apiKey,
                                                    @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey)throws DatagioException  {
        if ( metaHeader == null || metaHeader.equalsIgnoreCase("_all")){
            metaHeader = "MetaHeader";
        }
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return service.getMatrix( company, metaHeader);
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Collection<MetaHeader> search(@QueryParam(value = "simpleQuery") String simpleQuery,
                               @QueryParam(value = "company") String company,
                               @QueryParam(value = "fortress") String fortress,
                               @QueryParam(value = "type") String type,
                               String apiKey,
                               @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {

        Company abCompany = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        QueryParams queryParams = new QueryParams();
        queryParams.setSimpleQuery(simpleQuery);
        queryParams.setCompany(company);
        queryParams.setFortress(fortress);
        queryParams.setType(type);
        return mediationFacade.search(abCompany, queryParams);
    }

//    @ResponseBody
//    @RequestMapping(value = "/", method = RequestMethod.POST)
//    public Collection<MetaHeader> doQuery( @RequestBody QueryParams queryParams,
//                                           String apiKey,
//                                           @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey)throws DatagioException  {
//
//        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
//        // ToDo - obtain the index to search on
//        // Call the SearchGateway to obtain the Collection<String> of metakeys from the queryParams.
//        // Then return
//        // trackServer.getHeaders(Company company, Collection<String> toFind)
//        Collection<MetaHeader> results = null;
//        //return service.getMatrix( company, metaHeader);
//        return results;
//
//    }

}
