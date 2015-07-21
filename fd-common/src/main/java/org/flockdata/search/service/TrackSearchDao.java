/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.service;

import org.flockdata.model.Entity;
import org.flockdata.track.bean.SearchChangeBean;

import java.io.IOException;
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
     * @param searchChange values to update from
     */
    SearchChangeBean handle(SearchChangeBean searchChange) throws IOException;

    /**
     * locates a document by LogResultBean.searchKey
     *
     *
     * @param entity auditHeader
     * @return document context as bytes
     */
    Map<String, Object> findOne(Entity entity);

    void purgeCache() ;

    /**
     * Locates a specific key monitored by the entity.
     * <p/>
     * If ID is null then the call is the same as findOne(entity)
     * where the searchKey is taken to be LogResultBean.searchKey
     *
     * @return found track change or null if none
     */
    Map<String, Object> findOne(Entity entity, String id);

    /**
     * Removes a search document. Most of the time, the searchKey in the entity
     * is sufficient. However if you are tracking EVERY change in the search engine, then you
     * can delete a specific instance
     *
     */
    boolean delete(SearchChangeBean searchChange);

    Map<String, Object> ping();

    boolean ensureIndex(String indexName, String documentType) throws IOException;
}
