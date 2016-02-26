/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
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
@RequestMapping("${org.fd.search.system.api:api}/v1/query")
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

    @RequestMapping(value = "/keys", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
    public EntityKeyResults keys(@RequestBody QueryParams queryParams) throws FlockException {

        return searchService.doKeyQuery(queryParams);
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