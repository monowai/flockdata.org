package com.auditbucket.audit.repo.neo4j;

import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.repo.neo4j.model.AuditLog;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: mike
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface AuditLogRepo extends GraphRepository<AuditLog> {

    @Query(value = "start header=node({0}) match header-[:changedTo]->change return count(change)")
    int getLogCount(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header-[ct:changedTo]->change return change")
    Set<IAuditLog> getAuditLogs(Long auditHeaderID);

    @Query(value = "start header=node({0}) match header-[ct:changedTo]->change return change order by change.when DESC limit 1")
    AuditLog getLastChange(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start header=node({0}) match header-[ct:changedTo]->change where change.when >= {1} and change.when <= {2} return change ")
     Set<IAuditLog> getAuditLogs(Long auditHeaderID, Long from, Long to);
}
