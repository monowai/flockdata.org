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

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditSearchDao;
import com.auditbucket.dao.IAuditQueryDao;
import com.auditbucket.search.AuditChange;
import com.auditbucket.search.SearchResult;
import com.auditbucket.search.endpoint.IElasticSearchEP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: Mike Holdsworth
 * Date: 18/06/13
 * Time: 2:03 PM
 */
@Service
@MessageEndpoint
public class AbSearchService implements IElasticSearchEP {
    @Autowired
    private IAuditSearchDao auditSearch;

    @Autowired
    private IAuditQueryDao auditQuery;

    @Autowired
    private IAbEngineGateway searchResult;

    public Long getHitCount(String index) {
        return auditQuery.getHitCount(index);
    }

    //    @Transactional
    @ServiceActivator(inputChannel = "searchRequest")
    public void createSearchableChange(AuditChange thisChange) {
        SearchResult result;
        if (thisChange.getSearchKey() != null) {
            auditSearch.update(thisChange);
            result = new SearchResult(thisChange);
        } else {
            result = new SearchResult(auditSearch.save(thisChange));
        }
        searchResult.handleSearchResult(result);

    }

    @Transactional
    //@ServiceActivator(inputChannel = "esDelete")
    public void delete(IAuditHeader auditHeader) {
        auditSearch.delete(auditHeader, null);

    }

    public byte[] findOne(IAuditHeader header) {
        return auditSearch.findOne(header);
    }

    public byte[] findOne(IAuditHeader header, String id) {
        return auditSearch.findOne(header, id);
    }
}
