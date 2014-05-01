package com.auditbucket.geography.endpoint;

import com.auditbucket.geography.service.GeographyService;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

/**
 * Geography related functions
 *
 * User: mike
 * Date: 27/04/14
 * Time: 11:41 AM
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/geo")
public class GeographyEP {

    @Autowired
    GeographyService geoService;
    @Autowired
    RegistrationService regService;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Collection<Tag> findFortresses(String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        return geoService.findCountries(regService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey)));
    }

}
