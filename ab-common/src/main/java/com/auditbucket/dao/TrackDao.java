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

package com.auditbucket.dao;

import com.auditbucket.audit.bean.LogInputBean;
import com.auditbucket.audit.bean.MetaInputBean;
import com.auditbucket.audit.model.*;
import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.FortressUser;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * User: Mike Holdsworth
 * Date: 21/04/13
 * Time: 7:56 PM
 */
public interface TrackDao {

    String ping();

    public MetaHeader save(MetaHeader metaHeader);

    /**
     * Saves the header with the supplied DocumentType label
     * @param metaHeader
     * @param documentType
     * @return updated header
     */
    public MetaHeader save(MetaHeader metaHeader, DocumentType documentType);

    /**
     * @param key     GUID
     * @param inflate should all relationships be loaded
     * @return header in inflated or "default" state
     */
    public MetaHeader findHeader(String key, boolean inflate);

    /**
     * @param id audit Header PK
     * @return count of log records for the PK
     */
    public int getLogCount(Long id);

    public TrackLog getLastLog(Long auditHeaderID);

    Set<TrackLog> getLogs(Long headerKey, Date from, Date to);

    Set<TrackLog> getLogs(Long auditHeaderID);

    Iterable<MetaHeader> findByCallerRef(Long fortressId, String callerRef);

    MetaHeader findByCallerRefUnique(Long id, String sourceKey) throws DatagioException;

    MetaHeader findByCallerRef(Long fortressId, Long documentId, String callerRef);

    MetaHeader fetch(MetaHeader header);

    TxRef findTxTag(String txTag, Company company, boolean fetchHeaders);

    public TxRef beginTransaction(String id, Company company);

    public Map<String, Object> findByTransaction(TxRef txRef);

    TrackLog addLog(MetaHeader header, ChangeLog al, DateTime fortressWhen, TrackLog existingLog);

    TrackLog save(TrackLog log);

    public ChangeLog save(FortressUser fUser, LogInputBean input, TxRef tagRef, ChangeLog lastChange);

    MetaHeader create(MetaInputBean inputBean, FortressUser fu, DocumentType documentType) throws DatagioException;

    TrackLog getLog(Long logId);

    LogWhat getWhat(Long whatId);

    MetaHeader getHeader(Long id);

    ChangeLog fetch(ChangeLog lastChange);

    ChangeLog save(ChangeLog change, Boolean compressed);

    Set<MetaHeader> findHeadersByTxRef(Long txName);

    Collection<MetaHeader> findHeaders(Long fortressId, Long skipTo);

    Collection<MetaHeader> findHeaders(Long id, String docType, Long skipTo);

    void delete(ChangeLog currentChange);

    void makeLastChange(MetaHeader metaHeader, ChangeLog priorChange);

    void crossReference(MetaHeader header, Collection<MetaHeader> targets, String refName);

    Map<String,Collection<MetaHeader>> getCrossReference(Company company, MetaHeader header, String xRefName);

    Collection<MetaHeader> findHeaders(Company company, Collection<String> toFind);
}
