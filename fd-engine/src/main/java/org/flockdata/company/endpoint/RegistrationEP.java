/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.company.endpoint;

import org.flockdata.engine.configure.SecurityHelper;
import org.flockdata.helper.FlockException;
import org.flockdata.model.SystemUser;
import org.flockdata.registration.RegistrationBean;
import org.flockdata.registration.SystemUserResultBean;
import org.flockdata.registration.service.RegistrationService;
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
@RequestMapping("${org.fd.engine.system.api:api}/v1/profiles")
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