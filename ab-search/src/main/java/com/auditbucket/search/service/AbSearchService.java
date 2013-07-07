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
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

/**
 * User: mike
 * Date: 18/06/13
 * Time: 2:03 PM
 */
@Service
public class AbSearchService {
    @Autowired
    private IAuditSearchDao auditSearch;

    @Autowired
    private IAuditQueryDao auditQuery;


    public Long getHitCount(String index) {
        return auditQuery.getHitCount(index);
    }

    @Transactional
    IAuditChange updateSearchableChange(IAuditChange thisChange) {
        if (thisChange.getSearchKey() != null) {
            auditSearch.update(thisChange);
            return thisChange;
        } else {
            // Why would we have a missing search document? Probably because the fortress
            //  went from non searchable to searchable.
            //ToDo: looks dodgy
            IAuditChange change = createSearchableChange(thisChange);
            if (change != null)
                change.setSearchKey(change.getSearchKey());
            return change;
        }
    }


    @Transactional
    IAuditChange createSearchableChange(IAuditChange thisChange) {
        thisChange = auditSearch.save(thisChange);
        return thisChange;
    }

    @Transactional
    public void delete(IAuditHeader auditHeader, @NotNull @NotEmpty String key) {
        auditSearch.delete(auditHeader, key);

    }

//    public IAuditChange createSearchableChange(SearchDocumentBean searchDocumentBean) {
//        return createSearchableChange(searchDocumentBean.getAuditHeader(), searchDocumentBean.getDateTime(), searchDocumentBean.getWhat(), searchDocumentBean.getEvent());
//    }

    public byte[] findOne(IAuditHeader header) {
        return auditSearch.findOne(header);
    }

    public byte[] findOne(IAuditHeader header, String id) {
        return auditSearch.findOne(header, id);
    }
}
