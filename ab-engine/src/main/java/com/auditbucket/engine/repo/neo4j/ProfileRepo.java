package com.auditbucket.engine.repo.neo4j;

import com.auditbucket.engine.repo.neo4j.model.ProfileNode;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 3/10/14
 * Time: 4:51 PM
 */
public interface ProfileRepo extends GraphRepository<ProfileNode> {
}
