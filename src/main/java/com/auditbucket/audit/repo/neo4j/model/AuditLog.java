package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITxRef;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    @Fetch
    private FortressUser madeBy;

    @RelatedTo(elementClass = TxRef.class, type = "txIncludes", direction = Direction.INCOMING, enforceTargetType = true)
    private ITxRef txRef;

    static ObjectMapper om = new ObjectMapper();

    private long sysWhen;
    private String comment;
    private String event;

    private String jsonWhat;
    private String name;

    private Long when = 0l;

    @Indexed(indexName = "esKey")
    private String searchKey;


    protected AuditLog() {
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        sysWhen = now.toDate().getTime();
    }

    public AuditLog(IFortressUser madeBy, DateTime fortressWhen, String event, String jsonWhat) {
        this();
        sysWhen = DateTime.now().getMillis();

        this.madeBy = (FortressUser) madeBy;
        if (fortressWhen != null && fortressWhen.getMillis() != 0) {
            this.when = fortressWhen.getMillis();
        } else {
            this.when = sysWhen;
        }
        this.event = event;
        this.name = event + ":" + madeBy.getName();
        this.jsonWhat = jsonWhat;
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
        return searchKey;
    }

    public void setSearchKey(String key) {
        this.searchKey = key;
    }

    /**
     * @return the name of the event that caused this change
     */
    @JsonIgnore
    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getJsonWhat() {
        return jsonWhat;
    }

    private Map<String, Object> mWhat;

    public Map<String, Object> getWhat() {
        if (jsonWhat == null)
            return null;

        if (mWhat != null)
            return mWhat;
        try {
            mWhat = om.readValue(jsonWhat, Map.class);
        } catch (IOException e) {
            mWhat = new HashMap<String, Object>();
            mWhat.put("what", jsonWhat);
        }
        return mWhat;
    }

    public void setTxRef(ITxRef txRef) {
        this.txRef = txRef;
    }

    public String getEvent() {
        return event;
    }
}
