/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;

import java.util.Map;
import java.util.Set;

public interface AuditHeader {

    public abstract Long getId();

    public abstract Fortress getFortress();

    public abstract String getDocumentType();

    /**
     * @return Global Unique ID
     */
    public abstract String getAuditKey();

    /**
     * @return last fortress user to modify this record
     */
    public FortressUser getLastUser();

    /**
     * Last updated by AuditBucket
     *
     * @return long representing the date in UTC
     */
    public long getLastUpdated();

    public void setLastUser(FortressUser user);

    /**
     * @return fortress user who create the record
     */
    public FortressUser getCreatedBy();

    /**
     * Who created this record
     *
     * @param user Fortress User who created this record
     */
    public void setCreatedUser(FortressUser user);

    /**
     * @return the index name to use for subsequent changes
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
     * @return date this was created in fortress timezone
     */
    public Long getFortressCreated();

    /**
     * @return date created in AuditBucket in UTC
     */
    public Long getABCreated();

    public boolean isSearchSuppressed();

    public void suppressSearch(boolean searchSuppressed);

    void setSearchKey(String parentKey);

    /**
     * @return search engine key value
     */
    public String getSearchKey();

    String getCallerRef();

    public void setTagValues(Set<TagValue> tagValues);

    public Set<TagValue> getTagValues();

    Map<String, String> getTagMap();

    void setLastChange(AuditChange change);

    public AuditChange getLastChange();
}