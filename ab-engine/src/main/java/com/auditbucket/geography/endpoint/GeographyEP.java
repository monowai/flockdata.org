package com.auditbucket.geography.endpoint;

import com.auditbucket.geography.service.GeographyService;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.helper.FlockException;
import com.auditbucket.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Geography related functions
 *
 * User: mike
 * Date: 27/04/14
 * Time: 11:41 AM
 * To change this template use File | Settings | File Templates.
 */
@RestController
@RequestMapping("/geo")
public class GeographyEP {

    @Autowired
    GeographyService geoService;
    @Autowired
    RegistrationService regService;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    public Collection<Tag> findCountries(String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws FlockException {
        return geoService.findCountries(regService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey)));
    }

}
