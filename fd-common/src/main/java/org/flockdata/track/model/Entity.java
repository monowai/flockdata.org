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

package org.flockdata.track.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.joda.time.DateTime;

import java.util.Map;

public interface Entity {

    public Long getId();

    /**
     * Foreign computer system that owns this entity
     *
     * @return fortress
     */
    public Fortress getFortress();

    /**
     * Callers classification of this entity
     *
     * @return
     */
    public String getDocumentType();

    /**
     * @return Global Unique ID
     */
    public String getMetaKey();

    /**
     * @return last fortress user to modify this record
     */
    public FortressUser getLastUser();

    /**
     * Last updated by FlockData
     *
     * @return date in UTC
     */
    public Long getLastUpdate();

    public Long getFortressDateUpdated();

    /**
     * Who, in the foreign fortress, last changed this?
     *
     * @param user user
     */
    public void setLastUser(FortressUser user);

    /**
     * @return fortress user who create the record
     */
    public FortressUser getCreatedBy();

    public Object getProperty(String name);

    public Map<String, Object> getProperties();

    /**
     * @return unique identify the fortress recognises for the recordType.
     */
    public String getName();

    /**
     * alters the lastChange value
     */
    void bumpUpdate();

    /**
     * @return if this entity should not be made searchable
     */
    public boolean isSearchSuppressed();

    /**
     * @param searchSuppressed never index this entity in the search service
     */
    public void suppressSearch(boolean searchSuppressed);

    /**
     * Enables the entity to track the search services key. Don't bother setting if it's
     * the same as the metaKey
     * @param parentKey
     */
    void setSearchKey(String parentKey);

    /**
     * @return search engine key value
     */
    public String getSearchKey();

    /**
     * @return foreign key in the Fortress
     */
    String getCallerRef();

    /**
     * @return date created in FlockData UTC
     */
    long getWhenCreated();

    /**
     * @return date created in the foreign fortress; defaults to whenCreated if not supplied
     */
    @JsonIgnore
    DateTime getFortressDateCreated();

    void setCreatedBy(FortressUser thisFortressUser);

    String getEvent();

    void setLastChange(Log newChange);

    public Log getLastChange();

    void setFortressLastWhen(Long fortressWhen);

    String getCallerKeyRef();

    String getDescription();

    void bumpSearch();

    void addLabel(String label);

    boolean isNew();

    /**
     * Flags the entity as being new
     */
    void setNew();

    /**
     * Force the entity to a fixed state (for unit testing)
     * @param status true = new, false = existing
     */
    void setNew(boolean status);

    boolean setProperties(Map<String, Object> properties);

    void setName(String name);
}