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

package org.flockdata.search.base;

import java.io.IOException;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.EntityKeyResults;
import org.flockdata.search.EsSearchRequestResult;
import org.flockdata.search.QueryParams;
import org.flockdata.search.TagCloud;
import org.flockdata.search.TagCloudParams;

/**
 * @author mholdsworth
 * @tag Query, Search
 * @since 8/09/2014
 */
public interface QueryService {
  TagCloud getTagCloud(TagCloudParams tagCloudParams) throws NotFoundException, FlockException;

  Long getHitCount(String index) throws IOException;

  EsSearchRequestResult doFdViewSearch(QueryParams queryParams) throws FlockException;

  /**
   * Returns the "data" associated with the entity resolved from the queryParams
   *
   * @param queryParams key to search for
   * @return searchResult with the what Map populated
   * @throws FlockException business exception occurred
   * @see QueryParams
   * @see EsSearchRequestResult
   */
  EsSearchRequestResult doParametrizedQuery(QueryParams queryParams) throws FlockException;

  EntityKeyResults doKeyQuery(QueryParams queryParams) throws FlockException;

  String doSearch(QueryParams queryParams) throws FlockException;

  void getTags(String indexName);
}
