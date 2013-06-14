package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.data.neo4j.annotation.*;

import java.util.Date;

/**
 * User: mike
 * Date: 15/04/13
 * Time: 5:57 AM
 */
@RelationshipEntity(type = "changed", useShortNames = true)
public class AuditLog implements IAuditLog {

    @StartNode
    private FortressUser madeBy;

    @Fetch
    @Indexed(indexName = "changeHeader")
    @EndNode
    private AuditHeader auditHeader;

    @GraphId
    private Long id;
    private long sysWhen;
    private String comment;

    private String what;
    private String event;

    @Indexed(indexName = "changedWhen")
    private Long when = 0l;

    @Indexed(indexName = "esKey")
    private String changeKey;

    @Indexed(indexName = "txRef")
    private String txRef;


    protected AuditLog() {
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        sysWhen = now.toDate().getTime();
    }

    public AuditLog(IAuditHeader header, IFortressUser madeBy, DateTime when, String event, String what) {
        this();
        this.madeBy = (FortressUser) madeBy;
        auditHeader = (AuditHeader) header;
        if (when != null) {
            //auditWhen = new AuditWhen((AuditHeader) header, this, when.getMillis());
            this.when = when.getMillis();
        } else {
            //auditWhen = new AuditWhen((AuditHeader) header, this, sysWhen);
            this.when = sysWhen;
        }

        this.event = event;
        this.what = what;
        //auditWhen.setChange(this);
    }

    @JsonIgnore
    public IAuditHeader getHeader() {
        //return auditWhen.getAuditHeader();
        return auditHeader;
    }

    @JsonIgnore
    public long getId() {
        return id;
    }


    public IFortressUser getWho() {
        return madeBy;
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

    @JsonIgnore
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

    public void setTxRef(String txRef) {
        this.txRef = txRef;
    }
}
