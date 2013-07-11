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

package com.auditbucket.engine.service;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import org.springframework.integration.annotation.Gateway;

/**
 * Facades the call to the underlying auditbucket-search implementation.
 * User: Mike Holdsworth
 * Date: 6/07/13
 * Time: 2:31 PM
 */
public interface AuditSearchService {
    @Gateway(requestChannel = "searchMake")
    public IAuditChange createSearchableChange(IAuditChange searchDocumentBean);

    @Gateway(requestChannel = "searchUpdate")
    public void updateSearchableChange(IAuditChange sd);

    public void delete(IAuditHeader auditHeader, String searchKey);

    public byte[] findOne(IAuditHeader auditHeader, String searchKey);

    public byte[] findOne(IAuditHeader auditHeader);

    public Long getHitCount(String s);

}
