/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.search.service;

import com.auditbucket.dao.QueryDao;
import com.auditbucket.helper.FlockException;
import com.auditbucket.helper.NotFoundException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.model.TagCloud;
import com.auditbucket.search.model.TagCloudParams;
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
    @ServiceActivator(inputChannel = "doTagCloudQuery", outputChannel = "doTagCloudReply") // Subscriber
    public TagCloud getTagCloud(TagCloudParams tagCloudParams) throws NotFoundException {
        return queryDao.getCloudTag(tagCloudParams);
    }

    @Override
    public Long getHitCount(String index) {
        return queryDao.getHitCount(index);
    }

    @Override
    @ServiceActivator(inputChannel = "doMetaKeyQuery", outputChannel = "doMetaKeyReply") // Subscriber
    public EsSearchResult metaKeySearch(QueryParams queryParams) throws FlockException {
        return queryDao.doEntitySearch(queryParams);
    }


    @Override
    public String doSearch(QueryParams queryParams) throws FlockException {
        return queryDao.doSearch(queryParams);
    }

}
