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

    @Fetch
    @RelatedToVia(elementClass = AuditWhen.class, type = "changedWhen", direction = Direction.OUTGOING)
    private AuditWhen auditWhen;

    @GraphId
    private Long id;
    private long sysWhen;
    @Indexed(indexName = "changedWhen")
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
        this.madeBy = (FortressUser) madeBy;

        if (when != null)
            auditWhen = new AuditWhen((AuditHeader)header, this, when.getMillis());
        else
            auditWhen = new AuditWhen((AuditHeader)header,this, sysWhen);;

        this.event = event;
        this.what = what;
        auditWhen.setChange(this);
    }

    @JsonIgnore
    public IAuditHeader getHeader() {
        return auditWhen.getAuditHeader();
    }

    public AuditWhen getAuditWhen(){
        return auditWhen;
    }

    @JsonIgnore
    public long getId() {
        return id;
    }


    public IFortressUser getWho() {
        return madeBy;
    }

    public Date getWhen() {
        return new Date(auditWhen.getWhen());
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
