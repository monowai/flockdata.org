package com.auditbucket.audit.repo.neo4j;

import com.auditbucket.audit.repo.neo4j.model.AuditHeader;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * User: mike
 * Date: 14/04/13
 * Time: 8:00 PM
 */
public interface AuditHeaderRepo extends GraphRepository<AuditHeader> {

    @Query(elementClass = AuditHeader.class, value = "start n=node:clientRef(name = {0} ) " +
            "   match n<-[:audit]-fortress<-[:owns]-company " +
            "   where fortress.name= {1} and company.name ={2}" +
            "return n")
    AuditHeader findByClientRef(String clientRef, String fortress, String company);


}
