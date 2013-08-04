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

package com.auditbucket.search.endpoint;

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.search.AuditSearchChange;
import com.auditbucket.search.SearchResult;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

/**
 * User: Mike Holdsworth
 * Since: 9/07/13
 */
@Component
public interface ElasticSearchGateway {
    @Gateway(requestChannel = "searchRequest")
    public void createSearchableChange(AuditSearchChange thisChange);

    //@Gateway(requestChannel = "esDelete")
    public void delete(@Payload AuditHeader auditHeader);
}
