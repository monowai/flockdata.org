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

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.AuditSearchDao;
import com.auditbucket.dao.IAuditQueryDao;
import com.auditbucket.search.AuditSearchChange;
import com.auditbucket.search.SearchResult;
import com.auditbucket.search.endpoint.ElasticSearchGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 18/06/13
 * Time: 9:03 PM
 */
@Service
@MessageEndpoint
public class AbSearchService implements ElasticSearchGateway {
    @Autowired
    private AuditSearchDao auditSearch;

    @Autowired
    private IAuditQueryDao auditQuery;

    @Autowired(required = false)
    private AbEngineGateway engineGateway;

    private Logger logger = LoggerFactory.getLogger(ElasticSearchGateway.class);

    public Long getHitCount(String index) {
        return auditQuery.getHitCount(index);
    }

    @ServiceActivator(inputChannel = "makeSearchRequest") // Subscriber
    public void createSearchableChange(AuditSearchChange thisChange) {
        if (logger.isTraceEnabled())
            logger.trace("searchRequest received for " + thisChange);

        SearchResult result;
        if (thisChange.getSearchKey() != null) {
            auditSearch.update(thisChange);
            result = new SearchResult(thisChange);
        } else {
            result = new SearchResult(auditSearch.save(thisChange));
        }
        // Used to tie the fact that the doc was updated back to the engine
        result.setLogId(thisChange.getLogId());

        if (logger.isDebugEnabled())
            logger.debug("dispatching searchResult to ab-engine " + result);

        engineGateway.handleSearchResult(result);
    }

    public void delete(AuditHeader auditHeader) {
        auditSearch.delete(auditHeader, null);

    }

    public byte[] findOne(AuditHeader header) {
        return auditSearch.findOne(header);
    }

    public byte[] findOne(AuditHeader header, String id) {
        return auditSearch.findOne(header, id);
    }

    public Map<String, Object> getHealth() {
        return auditSearch.ping();
    }
}
