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

import com.auditbucket.audit.model.*;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.bean.AuditLogInputBean;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.FortressUser;
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

    public AuditHeader save(AuditHeader auditHeader);

    public AuditChange save(FortressUser fUser, AuditLogInputBean input, TxRef tagRef);

    /**
     * @param key GUID
     * @return AuditHeader in "default" state
     */
    public AuditHeader findHeader(String key);

    /**
     * @param key     GUID
     * @param inflate should all relationships be loaded
     * @return header in inflated or "default" state
     */
    public AuditHeader findHeader(String key, boolean inflate);

    /**
     * @param id audit Header PK
     * @return count of log records for the PK
     */
    public int getLogCount(Long id);

    public AuditLog getLastChange(Long auditHeaderID);

    Set<AuditLog> getAuditLogs(Long headerKey, Date from, Date to);

    Set<AuditLog> getAuditLogs(Long auditHeaderID);

    void delete(AuditChange auditLog);

    void delete(AuditHeader auditHeader);

    AuditHeader findHeaderByCallerRef(Long fortressId, String documentType, String callerRef);

    public void removeLastChange(AuditHeader header);

    AuditHeader fetch(AuditHeader header);

    TxRef findTxTag(String txTag, Company company, boolean fetchHeaders);

    public TxRef beginTransaction(String id, Company company);

    public Map<String, Object> findByTransaction(TxRef txRef);

    AuditLog addLog(AuditHeader header, AuditChange al, DateTime fortressWhen);

    void save(AuditLog log);

    String ping();

    AuditChange save(FortressUser fUser, AuditLogInputBean input);

    AuditHeader save(FortressUser fu, AuditHeaderInputBean inputBean, DocumentType documentType);

    AuditLog getChange(Long logId);
}
