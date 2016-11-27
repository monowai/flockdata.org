/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.geography.endpoint;

import org.flockdata.engine.configure.ApiKeyInterceptor;
import org.flockdata.geography.service.GeographyService;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.TagResultBean;
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
 * @author mholdsworth
 * @since 27/04/2014
 * @tag EndPoint, Geo, Country
 */
@RestController
@RequestMapping("${org.fd.engine.system.api:api}/v1/geo")
public class GeographyEP {

    private final GeographyService geoService;
    private final RegistrationService regService;

    @Autowired
    public GeographyEP(GeographyService geoService, RegistrationService regService) {
        this.geoService = geoService;
        this.regService = regService;
    }

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    public Collection<TagResultBean> findCountries(String apiKey, @RequestHeader(value = "api-key", required = false) String apiHeaderKey) throws FlockException {
        return geoService.findCountries(regService.resolveCompany(ApiKeyInterceptor.ApiKeyHelper.resolveKey(apiHeaderKey, apiKey)));
    }

}
