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

package com.auditbucket.dao;

import com.auditbucket.helper.FlockException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;
import com.auditbucket.search.model.TagCloud;
import com.auditbucket.search.model.TagCloudParams;

import java.io.IOException;

/**
 * User: Mike Holdsworth
 * Date: 28/04/13
 * Time: 8:44 PM
 */
public interface QueryDao {
    TagCloud getCloudTag(TagCloudParams tagCloudParams);

    long getHitCount(String index);

    EsSearchResult doEntitySearch(QueryParams queryParams) throws FlockException;

    String doSearch(QueryParams queryParams) throws FlockException;
}
