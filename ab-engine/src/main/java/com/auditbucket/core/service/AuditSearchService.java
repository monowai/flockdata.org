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

package com.auditbucket.core.service;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.search.AuditChange;
import com.auditbucket.search.SearchDocumentBean;
import org.springframework.stereotype.Service;

/**
 * Facades the call to the underlying auditbucket-search implementation.
 * User: Mike Holdsworth
 * Date: 6/07/13
 * Time: 2:31 PM
 */
@Service
public class AuditSearchService {
    public IAuditChange createSearchableChange(SearchDocumentBean searchDocumentBean) {
        if (searchDocumentBean.getAuditHeader().getFortress().isIgnoreSearchEngine())
            return null;
        IAuditChange change = new AuditChange(searchDocumentBean);
        return change;
    }

    public void updateSearchableChange(SearchDocumentBean sd) {


    }

    public void delete(IAuditHeader auditHeader, String searchKey) {


    }

    public byte[] findOne(IAuditHeader auditHeader, String searchKey) {
        return new byte[0];
    }

    public byte[] findOne(IAuditHeader auditHeader) {
        return new byte[0];
    }

    public Long getHitCount(String s) {
        return null;
    }

}
