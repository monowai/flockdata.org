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

package org.flockdata.search.endpoint;

import org.flockdata.helper.FlockException;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.search.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * ElasticSearch input end-point
 * User: Mike Holdsworth
 * Date: 7/07/13
 * Time: 10:03 PM
 */
@RequestMapping("/query")
@RestController
public class ElasticSearchEP {
    @Autowired
    QueryService searchService;


    @RequestMapping(value = "/", produces = "application/json", method = RequestMethod.POST)

    public String simpleQuery(@RequestBody QueryParams queryParams,
                            @RequestHeader(value = "Api-Key", required = false)
                            String apiHeaderKey) throws FlockException {

        return searchService.doSearch(queryParams);
    }

    @RequestMapping(value = "/metaKeys", produces = "application/json", method = RequestMethod.POST)

    public EsSearchResult metaKeys(@RequestBody QueryParams queryParams,
                              @RequestHeader(value = "Api-Key", required = false)
                              String apiHeaderKey) throws FlockException {

        return searchService.metaKeySearch(queryParams);
    }

}