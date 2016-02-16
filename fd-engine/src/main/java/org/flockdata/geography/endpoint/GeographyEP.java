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

package org.flockdata.geography.endpoint;

import org.flockdata.authentication.registration.service.RegistrationService;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.ApiKeyHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.model.Tag;
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
@RequestMapping("/geo")
public class GeographyEP {

    @Autowired
    GeographyService geoService;
    @Autowired
    RegistrationService regService;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    public Collection<Tag> findCountries(String apiKey, @RequestHeader(value = "api-key", required = false) String apiHeaderKey) throws FlockException {
        return geoService.findCountries(regService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey)));
    }

}
