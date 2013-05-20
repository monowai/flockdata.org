package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.registration.model.IFortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.data.neo4j.annotation.*;

import java.util.Date;

/**
 * User: mike
 * Date: 15/04/13
 * Time: 5:57 AM
 */
@RelationshipEntity(type = "auditChange")
public class AuditLog implements IAuditLog {

    @EndNode()
    @Fetch
    private IFortressUser createdBy;

    @Indexed(indexName = "changeHeader")
    @StartNode()
    private IAuditHeader auditHeader;

    @GraphId
    private Long id;
    private long sysWhen;
    @Indexed(indexName = "changedWhen")
    private long when;
    private String comment;

    private String what;
    private String event;
    @Indexed(indexName = "esKey")
    private String changeKey;

    protected AuditLog() {
        DateTime now =new DateTime().toDateTime(DateTimeZone.UTC);
        sysWhen = now.toDate().getTime();
    }

    public AuditLog(IAuditHeader header, IFortressUser user, DateTime when, String event, String what) {
        this();
        setHeader(header);
        createdBy = user;

        if (when != null)
            this.when = when.getMillis();
        else
            this.when = sysWhen;

        this.event = event;
        this.what = what;
    }

    @JsonIgnore
    public IAuditHeader getHeader() {
        return auditHeader;
    }

    @JsonIgnore
    public long getId() {
        return id;
    }

    protected void setHeader(IAuditHeader header) {
        this.auditHeader = header;
    }

    public String getWho() {
        return createdBy.getName();
    }

    public Date getWhen() {
        return new Date(when);
    }

    public Date getSysWhen() {
        return new Date(sysWhen);
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getKey() {
        return changeKey;
    }

    public void setKey(String key) {
        this.changeKey = key;
    }

    public String getEvent() {
        return event;
    }


    public String getWhat() {
        return what;
    }

}
