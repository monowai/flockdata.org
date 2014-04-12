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

package com.auditbucket.audit.model;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 26/04/13
 * Time: 12:26 PM
 */
public interface TrackSearchDao {

    /**
     * Rewrites an existing document
     *
     * @param auditChange values to update from
     */
    SearchChange update(SearchChange auditChange);

    /**
     * locates a document by LogResultBean.searchKey
     *
     * @param header auditHeader
     * @return document context as bytes
     */
    public byte[] findOne(MetaHeader header);

    /**
     * Locates a specific key monitored by the header.
     * <p/>
     * If ID is null then the call is the same as findOne(header)
     * where the searchKey is taken to be LogResultBean.searchKey
     *
     * @return found audit change or null if none
     */
    byte[] findOne(MetaHeader header, String id);

    /**
     * Removes a search document. Most of the time, the searchKey in the header
     * is sufficient. However if you are tracking EVERY change in the search engine, then you
     * can delete a specific instance
     *
     * @param header           LogResultBean that the change belongs to
     * @param existingIndexKey searchKey for the header to remove. if NULL, defaults to header.getSearchKey()
     */
    void delete(MetaHeader header, String existingIndexKey);

    Map<String, Object> ping();
}
