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

package com.auditbucket.core.endpoint;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.Payload;

/**
 * User: Mike Holdsworth
 * Date: 7/07/13
 * Time: 8:54 AM
 */
public interface AuditSearchGateway {

    @Gateway(requestChannel = "searchMake", replyChannel = "searchOutput")
    public IAuditChange createSearchableChange(@Payload IAuditChange thisChange);

    @Gateway(requestChannel = "searchUpdate", replyChannel = "searchOutput")
    public IAuditChange updateSearchableChange(@Payload IAuditChange thisChange);

    @Gateway(requestChannel = "searchDelete")
    public void delete(@Payload IAuditHeader auditHeader);
}
