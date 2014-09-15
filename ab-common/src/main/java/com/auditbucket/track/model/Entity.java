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

package com.auditbucket.track.model;

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;

public interface Entity {

    public Long getId();

    /**
     * Foreign computer system that owns this header
     *
     * @return fortress
     */
    public Fortress getFortress();

    /**
     * Callers classification of this header
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
     * Last updated by AuditBucket
     *
     * @return date in UTC
     */
    public Long getLastUpdate();

    public Long getFortressLastWhen();

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

    /**
     * @return the index this header belongs to
     */
    public String getIndexName();

    /**
     * @return unique identify the fortress recognises for the recordType.
     */
    public String getName();

    /**
     * alters the lastChange value
     */
    void bumpUpdate();

    /**
     * @return if this header should not be made searchable
     */
    public boolean isSearchSuppressed();

    /**
     * @param searchSuppressed never index this header in the search service
     */
    public void suppressSearch(boolean searchSuppressed);

    /**
     * Enables the header to track the search services key. Don't bother setting if it's
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
     * @return date created in AuditBucket UTC
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

}