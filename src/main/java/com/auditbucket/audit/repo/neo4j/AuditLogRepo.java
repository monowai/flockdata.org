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

    @Query(value = "start ah=node({0}) match ah-[:auditChange]->ae return count(ae)")
    int getLogCount(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start ah=node({0}) match ah-[ac:auditChange]->ae return ac")
    Set<IAuditLog> getAuditLogs(Long auditHeaderID);

    @Query(value = "start ah=node({0}) match ah-[ac:auditChange]->change return ac order by ac.when DESC limit 1")
    AuditLog getLastChange(Long auditHeaderID);

    @Query(elementClass = AuditLog.class, value = "start ah=node({0}) match ah-[ac:auditChange]->change where ac.when >= {1} and ac.when <= {2} return ac ")
    public Set<IAuditLog> getAuditLogs(Long auditHeaderID, Long from, Long to);
}
