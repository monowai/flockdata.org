package com.auditbucket.audit.repo.neo4j;

import com.auditbucket.audit.model.ITagRef;
import com.auditbucket.audit.repo.neo4j.model.TagRef;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 14/06/13
 * Time: 10:12 AM
 */
public interface AuditTXRepo extends GraphRepository<AuditTXRepo> {
    @Query(elementClass = TagRef.class, value = "start header=node({0}) match header<-[cw:changed|created]-byUser return cw")
    ITagRef getTxRef(Long auditHeaderID);

}
