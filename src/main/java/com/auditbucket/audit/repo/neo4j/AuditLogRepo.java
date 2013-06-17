package com.auditbucket.audit.repo.neo4j;

import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.repo.neo4j.model.AuditLog;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: mike
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface AuditLogRepo extends GraphRepository<AuditLog> {

    @Query(value = "start header=node({0}) match header<-[cw:changed]-byUser return count(cw)")
    int getLogCount(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header<-[cw:changed|created]-byUser return cw")
    Set<IAuditLog> getAuditLogs(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header<-[cw:changed]-byUser return cw order by cw.when DESC limit 1")
    AuditLog getLastChange(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header<-[cw:changed]-byUser where cw.when >= {1} and cw.when <= {2} return cw ")
    Set<IAuditLog> getAuditLogs(Long auditHeaderID, Long from, Long to);

    @Query(value = "start audit=node({0}) " +
            "match audit-[log:changed]-fortressUser " +
            "where log.txRef! = {1} " +
            "return audit, log order by log.sysWhen")
    EndResult<Map<String, Object>> findLogs(Collection<Long> headers, String txTagName);
}
