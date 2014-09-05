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

import com.auditbucket.dao.SchemaDao;
import com.auditbucket.helper.ApiKeyHelper;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.bean.TagInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.Tag;
import com.auditbucket.registration.service.RegistrationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * User: Mike Holdsworth
 * Since: 8/11/13
 */
@Controller
@RequestMapping("/tag")
public class TagEP {

    @Autowired
    com.auditbucket.track.service.TagService tagService;

    @Autowired
    SchemaDao schemaDao;

    @Autowired
    private RegistrationService registrationService;


    @ResponseBody
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.PUT)
    public Collection<TagInputBean> createTags(@RequestBody List<TagInputBean> tagInputs,
                                               String apiKey,
                                               @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException, ExecutionException, InterruptedException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));

        schemaDao.ensureUniqueIndexes(company, tagInputs, tagService.getExistingIndexes());
        try {
            tagService.createTagsNoRelationships(company, tagInputs);
        } catch (IOException e) {
            // Todo - how to handle??
            throw new DatagioException("Error processing your batch. Please run it again");
        }
        return tagService.processTags(company, tagInputs);

    }

    @ResponseBody
    @RequestMapping(value = "/", produces = "application/json", consumes = "application/json", method = RequestMethod.DELETE)
    public ResponseEntity<String> purgeUnusedConcepts(
                                               String apiKey,
                                               @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));

        tagService.purgeUnusedConcepts(company);
        return new ResponseEntity<>("Purged unused concepts", HttpStatus.ACCEPTED);

    }

    @ResponseBody
    @RequestMapping(value = "/{type}", produces = "application/json", consumes = "application/json", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteConcepts( @PathVariable("type") String type,
                                                              String apiKey,
            @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));

        tagService.purgeType(company, type);
        return new ResponseEntity<>("Purged unused concepts", HttpStatus.ACCEPTED);

    }


    @ResponseBody
    @RequestMapping(value = "/{type}", produces = "application/json", consumes = "application/json", method = RequestMethod.GET)
    public Collection<Tag> getTags(@PathVariable("type") String index,
                                   String apiKey,
                                   @RequestHeader(value = "Api-Key", required = false) String apiHeaderKey) throws DatagioException {
        Company company = registrationService.resolveCompany(ApiKeyHelper.resolveKey(apiHeaderKey, apiKey));
        return tagService.findTags(company, index);
    }
}
