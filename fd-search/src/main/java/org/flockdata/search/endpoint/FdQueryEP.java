/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.endpoint;

import org.flockdata.helper.FlockException;
import org.flockdata.search.EntityKeyResults;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.TagCloud;
import org.flockdata.search.TagCloudParams;
import org.flockdata.search.base.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ElasticSearch input end-point
 *
 * @author mholdsworth
 * @tag Search, Endpoint, Query
 * @since 7/07/2013
 */
@RequestMapping("${org.fd.search.system.api:api}/v1/query")
@RestController
public class FdQueryEP {
  private final QueryService searchService;

  @Autowired
  public FdQueryEP(@Qualifier("queryServiceEs") QueryService searchService) {
    this.searchService = searchService;
  }

  @PostMapping(value = "/", consumes = "application/json", produces = "application/json")
  public String simpleQuery(@RequestBody QueryParams queryParams) throws FlockException {

    return searchService.doSearch(queryParams);
  }

  @PostMapping(value = "/fdView")
  public EsSearchRequestResult fdViewQuery(@RequestBody QueryParams queryParams) throws FlockException {
    return searchService.doFdViewSearch(queryParams);
  }

  @PostMapping(value = "/keys")
  public EntityKeyResults keys(@RequestBody QueryParams queryParams) throws FlockException {
    return searchService.doKeyQuery(queryParams);
  }

  @PostMapping(value = "/data", consumes = "application/json", produces = "application/json")
  public EsSearchRequestResult dataContent(@RequestBody QueryParams queryParams) throws FlockException {
    return searchService.doParametrizedQuery(queryParams);
  }

  @PostMapping(value = "/tagCloud", consumes = "application/json", produces = "application/json")
  public TagCloud tagCloud(@RequestBody TagCloudParams queryParams) throws FlockException {
    return searchService.getTagCloud(queryParams);
  }

}