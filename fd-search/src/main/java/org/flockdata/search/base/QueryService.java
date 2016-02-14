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

package org.flockdata.search.base;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.model.*;

/**
 * User: mike
 * Date: 8/09/14
 * Time: 10:55 AM
 */
public interface QueryService {
    TagCloud getTagCloud(TagCloudParams tagCloudParams) throws NotFoundException;

    Long getHitCount(String index);

    EsSearchResult doFdViewSearch(QueryParams queryParams) throws FlockException;

    /**
     * Returns the "data" associated with the entity resolved from the queryParams
     * @param queryParams key to search for
     * @return searchResult with the what Map populated
     *
     * @throws FlockException
     */
    EsSearchResult doEntityQuery(QueryParams queryParams) throws FlockException;

    MetaKeyResults doMetaKeyQuery(QueryParams queryParams) throws FlockException;

    String doSearch(QueryParams queryParams) throws FlockException;

    void getTags(String indexName);
}
