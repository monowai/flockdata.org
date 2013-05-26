package com.auditbucket.audit.repo.neo4j.model;

import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

/**
 * User: mike
 * Date: 26/05/13
 * Time: 4:12 PM
 */
@RelationshipEntity(type = "changedWhen")
public class AuditWhen {

    @StartNode
    private AuditLog auditLog;

    @EndNode
    private AuditHeader auditHeader;
    @GraphId
    private Long id;

    private Long when =0l;
    public AuditWhen(){};
    public AuditWhen(AuditHeader header, AuditLog log, long millis) {
        this.auditHeader = header;
        this.auditLog = log;
        this.when = millis;
    }


    public Long getWhen() {
        return when;
    }

    public void setWhen(Long when) {
        this.when = when;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public AuditHeader getAuditHeader() {
        return auditHeader;
    }

    public void setChange(AuditLog auditLog) {
        this.auditLog = auditLog;
    }
}
