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

import com.auditbucket.helper.DatagioException;
import com.auditbucket.search.model.EsSearchResult;
import com.auditbucket.search.model.QueryParams;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 28/04/13
 * Time: 8:44 PM
 */
public interface QueryDao {
    long getHitCount(String index);

    EsSearchResult doMetaKeySearch(QueryParams queryParams) throws DatagioException;

    String doSearch(QueryParams queryParams) throws DatagioException;
}
