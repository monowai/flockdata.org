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
import org.flockdata.search.base.QueryService;
import org.flockdata.search.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * ElasticSearch input end-point
 * User: Mike Holdsworth
 * Date: 7/07/13
 * Time: 10:03 PM
 */
@RequestMapping("${fd-search.system.api}/v1/query")
@RestController
public class FdQueryEP {
    @Autowired
    @Qualifier("queryServiceEs")
    QueryService searchService;

    @RequestMapping(value = "/", consumes = "application/json", produces = "application/json",
            method = RequestMethod.POST)
    public String simpleQuery(@RequestBody QueryParams queryParams) throws FlockException {

        return searchService.doSearch(queryParams);
    }

    @RequestMapping(value = "/fdView", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public EsSearchResult fdViewQuery(@RequestBody QueryParams queryParams) throws FlockException {

        return searchService.doFdViewSearch(queryParams);
    }

    @RequestMapping(value = "/metaKeys", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public MetaKeyResults metaKeys(@RequestBody QueryParams queryParams) throws FlockException {

        return searchService.doMetaKeyQuery(queryParams);
    }

    @RequestMapping(value = "/data", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public EsSearchResult dataContent(@RequestBody QueryParams queryParams) throws FlockException {
        return searchService.doEntityQuery(queryParams);
    }

    @RequestMapping(value = "/tagCloud", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public TagCloud tagCloud(@RequestBody TagCloudParams queryParams) throws FlockException {
        return searchService.getTagCloud(queryParams);
    }

}