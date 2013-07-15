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

import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;

import java.util.Map;
import java.util.Set;

public interface IAuditHeader {

    public abstract Long getId();

    public abstract IFortress getFortress();

    public abstract String getDocumentType();

    /**
     * @return Global Unique ID
     */
    public abstract String getAuditKey();

    /**
     * @return last fortress user to modify this record
     */
    public IFortressUser getLastUser();

    public long getLastUpdated();

    public void setLastUser(IFortressUser user);

    /**
     * @return fortress user who create the record
     */
    public IFortressUser getCreatedBy();

    /**
     * Who created this record
     *
     * @param user Fortress User who created this record
     */
    public void setCreatedUser(IFortressUser user);

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

    Set<IAuditWhen> getAuditLogs();

    void setSearchKey(String parentKey);

    /**
     * @return search engine key value
     */
    public String getSearchKey();

    String getCallerRef();

    public Set<ITagValue> getTagValues();

    Map<String, Object> getTagMap();
}