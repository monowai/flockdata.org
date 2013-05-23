package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.Date;

/**
 * User: mike
 * Date: 15/04/13
 * Time: 5:57 AM
 */
@NodeEntity
public class AuditLog implements IAuditLog {

    @RelatedTo(elementClass = FortressUser.class, type = "madeBy", direction = Direction.OUTGOING)
    @Fetch
    private FortressUser madeBy;

    @RelatedTo(elementClass = AuditHeader.class, type = "changedTo", direction = Direction.INCOMING)
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

    public AuditLog(IAuditHeader header, IFortressUser madeBy, DateTime when, String event, String what) {
        this();
        setHeader(header);
        this.madeBy = (FortressUser) madeBy;

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

}
