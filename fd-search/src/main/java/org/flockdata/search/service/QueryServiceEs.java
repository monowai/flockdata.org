/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.search.service;

import org.flockdata.dao.QueryDao;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.base.QueryService;
import org.flockdata.search.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * User: Mike Holdsworth
 * Date: 18/06/13
 * Time: 9:03 PM
 */
@Service ("queryServiceEs")
public class QueryServiceEs implements QueryService {

    private final QueryDao queryDao;

    @Autowired
    public QueryServiceEs(QueryDao queryDao) {
        this.queryDao = queryDao;
    }

    @Override
    public TagCloud getTagCloud(TagCloudParams tagCloudParams) throws NotFoundException {
        return queryDao.getCloudTag(tagCloudParams);
    }

    @Override
    public Long getHitCount(String index) {
        return queryDao.getHitCount(index);
    }

    @Override
    public EsSearchResult doFdViewSearch(QueryParams queryParams) throws FlockException {
            return queryDao.doEntitySearch(queryParams);
    }

    public EntityKeyResults doKeyQuery(QueryParams queryParams) throws FlockException {
        return queryDao.doEntityKeySearch(queryParams);
    }

    @Override
    public EsSearchResult doEntityQuery(QueryParams queryParams) throws FlockException {
        // DAT-347 introduces ES as KV store
        return queryDao.doWhatSearch(queryParams);
    }

    @Override
    public String doSearch(QueryParams queryParams) throws FlockException {
        return queryDao.doSearch(queryParams);
    }

    @Override
    public void getTags(String indexName) {
        queryDao.getTags(indexName);
    }

}
