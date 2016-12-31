/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.search.base;

import org.flockdata.search.TagSearchChange;

import java.io.IOException;

/**
 * @author mholdsworth
 * @since 26/04/2013
 */
public interface TagChangeWriter {

    /**
     * Rewrites an existing document
     *
     * @param searchChange values to update from
     */
    TagSearchChange handle(TagSearchChange searchChange) throws IOException;

    /**
     * Removes a search document. Most of the time, the searchKey in the entity
     * is sufficient. However if you are tracking EVERY change in the search engine, then you
     * can delete a specific instance
     *
     */
    boolean delete(TagSearchChange searchChange);

}
