/*
 * Copyright (c) 2012-2014 "FlockData LLC"
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

import org.flockdata.search.model.EntitySearchChanges;
import org.flockdata.track.model.Entity;

import java.io.IOException;

/**
 * User: mike
 * Date: 8/09/14
 * Time: 10:57 AM
 */
public interface TrackService {

    org.flockdata.search.model.SearchResults createSearchableChange(EntitySearchChanges changes) throws IOException;

    Boolean createSearchableChange(byte[] bytes) throws IOException;

    void delete(Entity entity);

    byte[] findOne(Entity entity);

    byte[] findOne(Entity entity, String id);
}
