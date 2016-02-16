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

package org.flockdata.company.endpoint;

import org.flockdata.authentication.registration.bean.RegistrationBean;
import org.flockdata.authentication.registration.bean.SystemUserResultBean;
import org.flockdata.authentication.registration.service.RegistrationService;
import org.flockdata.configure.SecurityHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.model.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@RestController
// Customise a dispatcher in web.xml
@RequestMapping("/profiles")
public class RegistrationEP {

    @Autowired
    RegistrationService regService;

    @Autowired
    SecurityHelper secHelper;


    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.POST)

    public ResponseEntity<SystemUserResultBean> registerSystemUser(@RequestBody RegistrationBean regBean) throws FlockException {
        // curl -u admin:hackme -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/profiles/register -d '{"name":"mikey", "companyName":"Monowai Dev","password":"whocares"}'
        SystemUser su = regService.registerSystemUser( regBean);

        if (su == null)
            return new ResponseEntity<>(new SystemUserResultBean(su), HttpStatus.CONFLICT);

        return new ResponseEntity<>(new SystemUserResultBean(su), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)

    public SystemUserResultBean get(@RequestHeader(value = "api-key", required = false) String apiHeaderKey) throws FlockException {
        // curl -u batch:123 -X GET http://localhost:8080/ab/profiles/me/

        return new SystemUserResultBean(regService.getSystemUser(apiHeaderKey));
    }


}