package com.auditbucket.registration.repo.neo4j;

import com.auditbucket.registration.repo.neo4j.model.SystemId;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 27/06/13
 * Time: 12:24 PM
 */
public interface SystemRepository extends GraphRepository<SystemId> {
}
