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

package org.flockdata.search.service;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.search.model.TagCloud;
import org.flockdata.search.model.TagCloudParams;

/**
 * User: mike
 * Date: 8/09/14
 * Time: 10:55 AM
 */
public interface QueryService {
    TagCloud getTagCloud(TagCloudParams tagCloudParams) throws NotFoundException;

    Long getHitCount(String index);

    EsSearchResult metaKeySearch(QueryParams queryParams) throws FlockException;

    /**
     * Returns the "What" associated with the callerRef in the queryParams
     * @param queryParams key to search for
     * @return searchResult with the what Map populated
     *
     * @throws FlockException
     */
    EsSearchResult contentQuery(QueryParams queryParams) throws FlockException;

    String doSearch(QueryParams queryParams) throws FlockException;
}
