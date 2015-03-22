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

import org.flockdata.dao.QueryDao;
import org.flockdata.helper.FlockException;
import org.flockdata.helper.NotFoundException;
import org.flockdata.search.model.EsSearchResult;
import org.flockdata.search.model.QueryParams;
import org.flockdata.search.model.TagCloud;
import org.flockdata.search.model.TagCloudParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

/**
 * User: Mike Holdsworth
 * Date: 18/06/13
 * Time: 9:03 PM
 */
@Service
@MessageEndpoint
public class QueryServiceEs implements QueryService {

    @Autowired
    private QueryDao queryDao;

    @Override
    @ServiceActivator(inputChannel = "doTagCloudQuery", outputChannel = "tagCloudReply") // Subscriber
    public TagCloud getTagCloud(TagCloudParams tagCloudParams) throws NotFoundException {
        return queryDao.getCloudTag(tagCloudParams);
    }

    @Override
    public Long getHitCount(String index) {
        return queryDao.getHitCount(index);
    }

    @Override
    @ServiceActivator(inputChannel = "doMetaKeyQuery", outputChannel = "metaKeyReply") // Subscriber
    public EsSearchResult metaKeySearch(QueryParams queryParams) throws FlockException {
            return queryDao.doEntitySearch(queryParams);
    }

    @Override
    @ServiceActivator(inputChannel = "doContentQuery", outputChannel = "contentReply") // Subscriber
    public EsSearchResult contentQuery(QueryParams queryParams) throws FlockException {
        // DAT-347 how to route the response
        return queryDao.doWhatSearch(queryParams);
    }


    @Override
    public String doSearch(QueryParams queryParams) throws FlockException {
        return queryDao.doSearch(queryParams);
    }

}
