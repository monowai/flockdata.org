/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.registration.endpoint;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.bean.SystemUserResultBean;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@Controller
// Customise a dispatcher in web.xml
@RequestMapping("/profiles")
public class RegistrationEP {

    @Autowired
    RegistrationService regService;

    @RequestMapping(value = "/", consumes = "application/json", method = RequestMethod.POST)
    @ResponseBody
    @Secured({"ROLE_USER"})
    public ResponseEntity<SystemUserResultBean> registerSystemUser(@RequestBody RegistrationBean regBean) throws DatagioException {
        // curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/ab/profiles/register -d '{"name":"mikey", "companyName":"Monowai Dev","password":"whocares"}'
        SystemUser su = regService.registerSystemUser(regBean);

        if (su == null)
            return new ResponseEntity<>(new SystemUserResultBean(su), HttpStatus.CONFLICT);

        return new ResponseEntity<>(new SystemUserResultBean(su), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    @ResponseBody
    public SystemUser get() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8080/ab/profiles/me

        return regService.getSystemUser();
    }


}