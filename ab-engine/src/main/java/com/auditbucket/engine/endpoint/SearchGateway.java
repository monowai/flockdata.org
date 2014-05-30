/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
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

package com.auditbucket.engine.endpoint;

import com.auditbucket.search.model.MetaSearchChanges;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.SearchChange;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.Payload;

/**
 * User: Mike Holdsworth
 * Date: 7/07/13
 * Time: 8:54 AM
 */
public interface SearchGateway {

    @Gateway(requestChannel = "makeSearchRequest")
    public SearchChange createSearchableChange(@Payload MetaSearchChanges changes);

    @Gateway(requestChannel = "searchDelete")
    public void delete(@Payload MetaHeader metaHeader);

    //ToDo:
    // Add an HTTP gateway call to ElasticSearchEP.getMetaKeys(QueryParams);
}
