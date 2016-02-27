/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
