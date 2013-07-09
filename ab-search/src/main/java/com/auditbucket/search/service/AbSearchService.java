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

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditSearchDao;
import com.auditbucket.dao.IAuditQueryDao;
import com.auditbucket.search.endpoint.IElasticSearchEP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Payload;
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


    public Long getHitCount(String index) {
        return auditQuery.getHitCount(index);
    }

    @Transactional
    @ServiceActivator(inputChannel = "esMake", outputChannel = "searchOutput")
    public IAuditChange createSearchableChange(@Payload IAuditChange thisChange) {
        thisChange = auditSearch.save(thisChange);
        return thisChange;
    }

    @Transactional
    @ServiceActivator(inputChannel = "esUpdate", outputChannel = "searchOutput")
    public IAuditChange updateSearchableChange(IAuditChange thisChange) {
        if (thisChange.getSearchKey() != null) {
            auditSearch.update(thisChange);
            return thisChange;
        } else {
            // Why would we have a missing search document? Probably because the fortress
            //  went from non searchable to searchable.
            IAuditChange change = createSearchableChange(thisChange);
            if (change != null)
                change.setSearchKey(change.getSearchKey());
            return change;
        }
    }


    @Transactional
    @ServiceActivator(inputChannel = "esDelete")
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
