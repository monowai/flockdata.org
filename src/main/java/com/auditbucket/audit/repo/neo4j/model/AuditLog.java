package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Date;

/**
 * User: mike
 * Date: 15/04/13
 * Time: 5:57 AM
 */
@NodeEntity(useShortNames = true)
public class AuditLog implements IAuditLog {
    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUser.class, type = "changed", direction = Direction.INCOMING, enforceTargetType = true)
    private FortressUser madeBy;

    @RelatedTo(elementClass = TxRef.class, type = "txIncludes", direction = Direction.INCOMING, enforceTargetType = true)
    private ITxRef txRef;


    private long sysWhen;
    private String comment;

    private String what;
    private String event;

    private Long when = 0l;

    @Indexed(indexName = "esKey")
    private String changeKey;


    protected AuditLog() {
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        sysWhen = now.toDate().getTime();
    }

    public AuditLog(IFortressUser madeBy, DateTime fortressWhen, String event, String what) {
        this();
        sysWhen = DateTime.now().getMillis();

        this.madeBy = (FortressUser) madeBy;
        if (fortressWhen != null && fortressWhen.getMillis() != 0) {
            this.when = fortressWhen.getMillis();
        } else {
            this.when = sysWhen;
        }

        this.event = event;
        this.what = what;
    }

    @JsonIgnore
    public IAuditHeader getHeader() {
        //return auditHeader;
        return null;
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
    public String getSearchKey() {
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

    public void setTxRef(ITxRef txRef) {
        this.txRef = txRef;
    }
}
