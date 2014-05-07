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
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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


    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public String simpleQuery(@RequestBody QueryParams queryParams,
                            @RequestHeader(value = "Api-Key", required = false)
                            String apiHeaderKey) throws DatagioException {

        return searchService.doSearch(queryParams);
    }

    @RequestMapping(value = "/metaKeys", produces = "application/json", method = RequestMethod.POST)
    @ResponseBody
    public EsSearchResult metaKeys(@RequestBody QueryParams queryParams,
                              @RequestHeader(value = "Api-Key", required = false)
                              String apiHeaderKey) throws DatagioException {

        return searchService.metaKeySearch(queryParams);
    }

}