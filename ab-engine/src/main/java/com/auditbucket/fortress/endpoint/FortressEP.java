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

package com.auditbucket.fortress.endpoint;

import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.FortressInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.FortressService;
import com.auditbucket.track.model.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 4/05/13
 * Time: 8:23 PM
 */
@Controller
@RequestMapping("/fortress")
public class FortressEP {

    @Autowired
    FortressService fortressService;

    @Autowired
    CompanyService companyService;

    @Autowired
    SecurityHelper securityHelper;

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Collection<Fortress> findFortresses(String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        // curl -u mike:123 -X GET  http://localhost:8080/ab/company/Monowai/fortresses
        Company company = securityHelper.getCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return fortressService.findFortresses(company);
    }

    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Fortress> registerFortress(@RequestBody FortressInputBean fortressInputBean, String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = securityHelper.getCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        Fortress fortress = fortressService.registerFortress(company, fortressInputBean, true);
        fortressInputBean.setFortressKey(fortress.getFortressKey());
        return new ResponseEntity<>(fortress, HttpStatus.CREATED);

    }

    @RequestMapping(value = "/{fortressName}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Fortress> getFortress(@PathVariable("fortressName") String fortressName, String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        // curl -u mike:123 -X GET  http://localhost:8080/ab/fortress/ABC
        Company company = securityHelper.getCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        Fortress fortress = fortressService.findByName(company, fortressName);
        if (fortress == null)
            return new ResponseEntity<>(fortress, HttpStatus.NOT_FOUND);
        else
            return new ResponseEntity<>(fortress, HttpStatus.OK);
    }

    @RequestMapping(value = "/{fortressName}/{userName}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<FortressUser> getFortressUsers(@PathVariable("fortressName") String fortressName, @PathVariable("userName") String userName,
                                                         String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = securityHelper.getCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));

        FortressUser result = null;
        Fortress fortress = fortressService.findByName(company, fortressName);

        if (fortress == null) {
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(fortressService.getFortressUser(fortress, userName), HttpStatus.OK);
    }

    @RequestMapping(value = "/{fortressName}/docs", method = RequestMethod.GET)
    @ResponseBody
    public Collection<DocumentType> getDocumentTypes(String fortressName,
                                                     String apiKey, @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = securityHelper.getCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return fortressService.getFortressDocumentsInUse(company, fortressName);
    }
}