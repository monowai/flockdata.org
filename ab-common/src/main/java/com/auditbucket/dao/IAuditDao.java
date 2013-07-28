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

package com.auditbucket.dao;

import com.auditbucket.audit.model.IAuditChange;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.registration.model.ICompany;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 21/04/13
 * Time: 7:56 PM
 */
public interface IAuditDao {

    public IAuditHeader save(IAuditHeader auditHeader);

    public IAuditChange save(IAuditChange auditLog);

    public ITxRef save(ITxRef tagRef);

    /**
     * @param key GUID
     * @return AuditHeader in "default" state
     */
    public IAuditHeader findHeader(String key);

    /**
     * @param key     GUID
     * @param inflate should all relationships be loaded
     * @return header in inflated or "default" state
     */
    public IAuditHeader findHeader(String key, boolean inflate);

    /**
     * @param id audit Header PK
     * @return count of log records for the PK
     */
    public int getLogCount(Long id);

    public IAuditLog getLastChange(Long auditHeaderID);

    /**
     * locates a specific change in auditBucket.
     * <p/>
     * This is used when syncing a change between ab-search and ab-engine, or to
     * reprocess changes that may not have been sync'd
     *
     * @param auditHeaderID PK
     * @param sysWhen       time auditBucket processed the change
     * @return the change for an exact match
     */
    public IAuditLog getChange(Long auditHeaderID, long sysWhen);

    Set<IAuditChange> getAuditLogs(Long headerKey, Date from, Date to);

    Set<IAuditChange> getAuditLogs(Long auditHeaderID);

    void delete(IAuditChange auditLog);

    void delete(IAuditHeader auditHeader);

    IAuditHeader findHeaderByCallerRef(Long fortressId, String documentType, String callerRef);

    public void removeLastChange(IAuditHeader header);

    IAuditHeader fetch(IAuditHeader header);

    ITxRef findTxTag(String txTag, ICompany company, boolean fetchHeaders);

    public ITxRef beginTransaction(String id, ICompany company);

    public Map<String, Object> findByTransaction(ITxRef txRef);

    void addChange(IAuditHeader header, IAuditChange al, DateTime dateWhen);

    void save(IAuditLog log);

    String ping();
}
