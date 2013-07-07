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

package com.auditbucket.core.repo.neo4j;

import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.core.repo.neo4j.model.AuditLog;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: mike
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface AuditLogRepo extends GraphRepository<AuditLog> {

    @Query(value = "start auditHeader=node({0}) match auditHeader-[cw:logged]->auditLog return count(cw)")
    int getLogCount(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header-[cw:changed|created]-byUser return cw")
    Set<IAuditLog> getAuditLogs(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header-[log:logged]->auditLog return auditLog order by log.sysWhen DESC limit 1")
    AuditLog getLastChange(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header-[log:logged]->auditLog where log.fortressWhen >= {1} and log.fortressWhen <= {2} return auditLog ")
    Set<IAuditLog> getAuditLogs(Long auditHeaderID, Long from, Long to);

    @Query(elementClass = AuditLog.class, value = "start audit=node({0}) " +
            "   MATCH audit-[l:logged]->auditLog " +
            "return auditLog  order by l.sysWhen desc")
    Set<IAuditLog> findAuditLogs(Long auditHeaderID);

//    @Query(value = "start audit=node({0}) " +
//            "match audit-[log:changed]-fortressUser " +
//            "where log.txRef! = {1} " +
//            "return audit, log order by log.sysWhen")
//    EndResult<Map<String, Object>> findLogs(Collection<Long> headers, String txTagName);
}
