package com.auditbucket.registration.repo.neo4j;

import com.auditbucket.registration.repo.neo4j.model.Tag;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 26/06/13
 * Time: 8:35 PM
 */

public interface TagRepository extends GraphRepository<Tag> {
    @Query(value = "start n=node({1}) " +
            "   MATCH n-[:tags]->tag " +
            "   where tag.name ={0} " +
            "  return tag")
    Tag findCompanyTag(String tagName, Long companyId);
}
