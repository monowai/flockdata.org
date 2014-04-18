package com.auditbucket.engine.endpoint;

import com.auditbucket.engine.service.MatrixService;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.service.RegistrationService;
import com.auditbucket.track.query.MatrixResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

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

    @ResponseBody
    @RequestMapping(value = "/matrix/{metaHeader}", method = RequestMethod.GET)
    public Collection<MatrixResult> getMatrix(@PathVariable("metaHeader") String metaHeader,
                                              String apiKey,
                                              @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey)throws DatagioException  {
        if ( metaHeader == null || metaHeader.equalsIgnoreCase("_all")){
            metaHeader = "MetaHeader";
        }
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return service.getMatrix( company, metaHeader);
    }

}
