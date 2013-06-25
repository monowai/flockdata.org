package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.IAuditWhen;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.neo4j.annotation.*;

/**
 * User: mike
 * Date: 26/05/13
 * Time: 4:12 PM
 */
@RelationshipEntity(type = "logged")
public class AuditWhen implements IAuditWhen {
    @GraphId
    private Long id;

    @EndNode
    private AuditLog auditLog;

    @StartNode
    private AuditHeader auditHeader;

    @Indexed(indexName = "sysWhen", numeric = true)
    private Long sysWhen = 0l;

    @Indexed(indexName = "fortressWhen")
    private Long fortressWhen = 0l;


    protected AuditWhen() {
    }

    public AuditWhen(IAuditHeader header, IAuditLog log) {
        this();
        this.auditHeader = (AuditHeader) header;
        this.auditLog = (AuditLog) log;
        // ToDo: denormalisation here; storing the times in the relationships and the node
        this.sysWhen = log.getSysWhen().getTime();
        this.fortressWhen = log.getWhen().getTime();
    }


    public Long getSysWhen() {
        return sysWhen;
    }

    public Long getFortressWhen() {
        return fortressWhen;
    }

    void setSysWhen(Long sysWhen) {
        this.sysWhen = sysWhen;
    }

    @JsonIgnore
    public IAuditLog getAuditLog() {
        return auditLog;
    }

    @JsonIgnore
    public IAuditHeader getAuditHeader() {
        return auditHeader;
    }

    public void setChange(AuditLog auditLog) {
        this.auditLog = auditLog;
    }
}
