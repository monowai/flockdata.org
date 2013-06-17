package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditLog;
import com.auditbucket.audit.model.ITagRef;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User: mike
 * Date: 14/04/13
 * Time: 10:56 AM
 */
@NodeEntity
public class AuditHeader implements IAuditHeader {

    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUser.class, type = "created", direction = Direction.INCOMING, enforceTargetType = true)
    private FortressUser createdBy;

    @RelatedTo(elementClass = FortressUser.class, type = "lastChanged", direction = Direction.OUTGOING)
    private FortressUser lastWho;

    @RelatedTo(elementClass = Fortress.class, type = "audit", direction = Direction.INCOMING)
    private Fortress fortress;

    @RelatedToVia(elementClass = AuditLog.class, type = "changed", direction = Direction.INCOMING)
    private Set<IAuditLog> auditLogs = null;

    @RelatedTo(elementClass = TagRef.class, type = "txIncludes", direction = Direction.INCOMING)
    private Set<ITagRef> txTags = null;

    public static final String UUID_KEY = "auditKey";
    @Indexed(indexName = UUID_KEY, unique = true)
    private String auditKey;

    @Indexed(indexName = "clientRef")
    private String name;
    private long dateCreated;

    private String dataType;

    private long fortressDate;
    long lastUpdated = 0;
    String searchKey = null;

    AuditHeader() {
        auditKey = UUID.randomUUID().toString();
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.dateCreated = now.toDate().getTime();
        this.lastUpdated = dateCreated;
    }

    public AuditHeader(@NotNull IFortressUser createdBy, @NotNull AuditHeaderInputBean auditInput) {
        this();
        String eventType = auditInput.getRecordType();
        String clientRef = auditInput.getCallerRef();
        Date when = auditInput.getWhen();
        if (when == null)
            fortressDate = dateCreated;
        else
            fortressDate = when.getTime();

        this.createdBy = (FortressUser) createdBy;
        this.lastWho = (FortressUser) createdBy;
        this.fortress = (Fortress) createdBy.getFortress();
        this.dataType = (eventType != null ? eventType.toLowerCase() : "");
        this.name = (clientRef == null ? null : (eventType + "." + clientRef).toLowerCase());


    }


    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    public String getAuditKey() {
        return auditKey;
    }

    @Override
    public IFortress getFortress() {
        return fortress;
    }

    @Override
    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public IFortressUser getLastUser() {
        return lastWho;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUser(IFortressUser user) {
        lastWho = (FortressUser) user;
    }

    @Override
    public IFortressUser getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedUser(IFortressUser user) {
        createdBy = (FortressUser) user;
    }

    private void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated.getTime();
    }

    @Override
    @JsonIgnore
    public String getIndexName() {
        if (fortress != null)
            return new StringBuilder().append(fortress.getCompany().getName().toLowerCase()).append(".").append(fortress.getName().toLowerCase()).toString();
        else
            return null;
    }

    /**
     * @return Date created in the fortress
     */
    public Date getFortressDate() {
        return new Date(fortressDate);
    }

    /**
     * @return date created in AuditBucket
     */
    public Date getDateCreated() {
        return new Date(dateCreated);
    }

    @Override
    public String toString() {
        return "AuditHeader{" +
                "id=" + id +
                ", auditKey='" + auditKey + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public void bumpUpdate() {
        lastUpdated = System.currentTimeMillis();
    }

    @Override
    public Set<IAuditLog> getAuditLogs() {
        return auditLogs;
    }

    @JsonIgnore
    public void setSearchKey(String parentKey) {
        this.searchKey = parentKey;
    }

    @Override
    public void addTxTag(ITagRef tag) {
        if (tag == null)
            return;
        getTxTags().add(tag);
    }

    public Set<ITagRef> getTxTags() {
        if (txTags == null)
            txTags = new HashSet<ITagRef>();
        return txTags;
    }
}
