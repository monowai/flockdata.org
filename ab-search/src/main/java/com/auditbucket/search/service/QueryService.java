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
import com.auditbucket.track.model.TrackSearchDao;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 18/06/13
 * Time: 9:03 PM
 */
@Service
@MessageEndpoint
public class QueryService  {
    @Autowired
    private TrackSearchDao trackDao;

    @Autowired
    private QueryDao queryDao;

    private Logger logger = LoggerFactory.getLogger(QueryService.class);

    public String doSearch(String index, String query){
        return queryDao.doSearch(index, query);
    }

    public Collection<String> doMetaKeySearch(String index, String query){
        return queryDao.doMetaKeySearch(index, query);
    }

    public Long getHitCount(String index) {
        return queryDao.getHitCount(index);
    }

    @ServiceActivator(inputChannel = "makeSendSearchRequest",outputChannel = "makeSendSearchReply") // Subscriber
    public EsSearchResult search(QueryParams queryParams) {
        logger.info("searchRequest received for {}", queryParams);
        Collection<String> results = queryDao.doMetaKeySearch("ab." + queryParams.getCompany() + "." + queryParams.getFortress(), queryParams.getSimpleQuery());
        return new EsSearchResult(results);
    }


}