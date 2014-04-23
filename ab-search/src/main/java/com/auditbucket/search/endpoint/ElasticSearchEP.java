/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

package com.auditbucket.search.endpoint;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.service.QueryService;
import com.auditbucket.search.service.SearchAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * ElasticSearch input end-point
 * User: Mike Holdsworth
 * Date: 7/07/13
 * Time: 10:03 PM
 */
@RequestMapping("/query")
@Controller
public class ElasticSearchEP {
    @Autowired
    QueryService searchService;

    @Autowired
    SearchAdmin searchAdmin;

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    @ResponseBody
    String ping() throws Exception {
        // curl -X GET http://localhost:8081/ab-search/v1/ping
        return "Pong!";
    }

    @RequestMapping(value = "/health", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getHealth() throws Exception {
        // curl -u mike:123 -X GET http://localhost:8081/ab-search/v1/health
        return searchAdmin.getHealth();
    }

    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public String simpleQuery(@RequestBody QueryParams queryParams,
                            @RequestHeader(value = "Api-Key", required = false)
                            String apiHeaderKey) throws DatagioException {

        return searchService.doSearch("ab.*", queryParams.getSimpleQuery());
        // curl -u mike:123 -X GET http://localhost:8081/ab-search/v1/health
        //return searchAdmin.getHealth();
    }

    @RequestMapping(value = "/metaKeys", produces = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public Collection<String> metaKeys(@RequestBody QueryParams queryParams,
                              @RequestHeader(value = "Api-Key", required = false)
                              String apiHeaderKey) throws DatagioException {

        return searchService.doMetaKeySearch("ab.*", queryParams.getSimpleQuery());
        // curl -u mike:123 -X GET http://localhost:8081/ab-search/v1/health
        //return searchAdmin.getHealth();
    }

}