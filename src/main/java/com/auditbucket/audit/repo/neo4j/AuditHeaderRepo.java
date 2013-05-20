package com.auditbucket.audit.repo.neo4j;

import com.auditbucket.audit.repo.neo4j.model.AuditHeader;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface AuditHeaderRepo extends GraphRepository<AuditHeader> {

}
