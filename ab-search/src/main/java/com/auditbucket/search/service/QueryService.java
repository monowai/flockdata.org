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
import com.auditbucket.helper.DatagioException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
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
public class QueryService  {

    @Autowired
    private QueryDao queryDao;

    public Long getHitCount(String index) {
        return queryDao.getHitCount(index);
    }

    @ServiceActivator(inputChannel = "doMetaKeyQuery",outputChannel = "doMetaKeyReply") // Subscriber
    public EsSearchResult metaKeySearch(QueryParams queryParams) throws DatagioException {
        return queryDao.doMetaKeySearch(queryParams);
    }


    public String doSearch(QueryParams queryParams) throws DatagioException {
        return queryDao.doSearch(queryParams);
    }

}
