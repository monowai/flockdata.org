/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.geography.endpoint;

import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Tag;
import org.flockdata.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("${org.fd.engine.system.api:api}/v1/geo")
public class GeographyEP {

    @Autowired
    GeographyService geoService;
    @Autowired
    RegistrationService regService;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    public Collection<Tag> findCountries(String apiKey, @RequestHeader(value = "api-key", required = false) String apiHeaderKey) throws FlockException {
        return geoService.findCountries(regService.resolveCompany(ApiKeyInterceptor.ApiKeyHelper.resolveKey(apiHeaderKey, apiKey)));
    }

}
