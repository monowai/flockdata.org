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

package org.flockdata.data;

import org.joda.time.DateTime;

import java.util.Map;

/**
 * Container representing a uniquely identifiable piece of data
 * @author mike
 * @tag Entity
 * @since 1/01/17
 */
public interface Entity {
    /**
     * @return FD internally unique ID
     */
    Long getId();

    /**
     * @return GUID
     */
    String getKey();

    String getName();

    /**
     * returns lower case representation of the documentType.name
     */
    String getType();

    FortressUser getLastUser();

    Long getLastUpdate();

    FortressUser getCreatedBy();

    /**
     * query the user defined properties
     * @param key
     * @return value associated with the key
     */
    Object getProperty(String key);

    /**
     *
     * @return user defined properties to be recorded against the entity
     */
    Map<String, Object> getProperties();

    /**
     *
     * @return is indexing this entity in the search system suppressed
     */
    boolean isSearchSuppressed();

    /**
     *
     * @return unique identifier within the search service
     */
    String getSearchKey();

    /**
     * @return callers unique identifier for the entity
     */
    String getCode();

    long getDateCreated();

    /**
     * @return nicely formatted date and time based on when this was created in the owning fortress (fortressCreate)
     */
    DateTime getFortressCreatedTz();

    /**
     *
     * @return is this entity new to FlockData?
     */
    boolean isNewEntity();

    /**
     *
     * @return true if there are no logs for this entity
     */
    boolean isNoLogs();

    /**
     *
     * @return computer system that owns this entity
     */
    Fortress getFortress();

    /**
     *
     * @return the segment that this entity exist in
     */
    Segment getSegment();

    /**
     *
     * @return last known event to occur against this entity
     */
    String getEvent();

    /**
     * Flags the entity as having been affected by search. Can be used for testing when waiting
     * for a count to be increased
     *
     * @return current search count
     */
    Integer getSearch();

    DateTime getFortressUpdatedTz();
}
