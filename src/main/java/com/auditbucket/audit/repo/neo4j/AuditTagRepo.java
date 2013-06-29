package com.auditbucket.audit.repo.neo4j;

import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.audit.repo.neo4j.model.AuditTagValue;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * User: mike
 * Date: 28/06/13
 * Time: 2:56 PM
 */
public interface AuditTagRepo extends GraphRepository<AuditTagValue> {

    @Query(elementClass = AuditTagValue.class, value = "start tag=node({0}) match tag-[tags:tagValue]->auditHeader where tags.tagValue={1} return tags")
    Set<ITagValue> findTagValues(Long tagId, String tagValue);
}
