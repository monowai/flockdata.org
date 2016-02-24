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

package org.flockdata.dao;

import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.model.*;

/**
 * User: Mike Holdsworth
 * Date: 28/04/13
 * Time: 8:44 PM
 */
public interface QueryDao {
    TagCloud getCloudTag(TagCloudParams tagCloudParams) throws NotFoundException;

    long getHitCount(String index);

    /**
     * Returns only keys
     * @param queryParams
     * @return
     */
    EntityKeyResults doEntityKeySearch(QueryParams queryParams ) throws FlockException;

    /**
     * FD-View search results
     *
     * @param queryParams
     * @return
     * @throws FlockException
     */
    EsSearchResult doEntitySearch(QueryParams queryParams) throws FlockException;

    // Treating Es like a current state KV store
    EsSearchResult doWhatSearch(QueryParams queryParams) throws FlockException;

    String doSearch(QueryParams queryParams) throws FlockException;

    void getTags(String indexName);
}
