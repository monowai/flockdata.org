package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.Date;
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


    @RelatedTo(elementClass = FortressUser.class, type = "createdBy", direction = Direction.INCOMING)
    @Fetch
    private FortressUser createdBy;

    @RelatedTo(elementClass = FortressUser.class, type = "lastWho", direction = Direction.OUTGOING)
    @Fetch
    private FortressUser lastWho;

    @RelatedTo(elementClass = Fortress.class, type = "audit", direction = Direction.INCOMING)
    @Fetch
    private Fortress fortress;

//    @RelatedToVia(type = "changedWhen")
//    private Set<AuditLog> auditLogs = null;


    public static final String UUID_KEY = "uid";

    @Indexed(indexName = UUID_KEY, unique = true)
    private String uid;

    private String name;
    private long dateCreated;
    private String dataType;
    private long fortressDate;
    private String comment;
    long lastUpdated = 0;

    @Indexed(indexName = "clientRef")
    private String clientRef;


    AuditHeader() {
        uid = UUID.randomUUID().toString();
        this.name = uid;
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.dateCreated = now.toDate().getTime();
        this.lastUpdated = dateCreated;
    }

    public AuditHeader(IFortressUser createdBy, String dataType, DateTime when, String clientRef) {
        this();
        this.createdBy = (FortressUser) createdBy;
        this.lastWho = (FortressUser) createdBy;
        this.fortress = (Fortress) createdBy.getFortress();
        this.fortressDate = when.toDate().getTime();
        this.dataType = dataType;
        this.clientRef = clientRef;
    }

    public Long getId() {
        return id;  //To change body of implemented methods use File | Settings | File Templates.
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
    public String getUID() {
        return uid;
    }

    @Override
    public IFortressUser getLastUser() {
        return lastWho;
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
    public String getIndexName() {
        if (fortress != null)
            return new StringBuilder().append(fortress.getCompany().getName().toLowerCase()).append(".").append(fortress.getName().toLowerCase()).toString();
        else
            return null;
    }

    /**
     * @return date created in AuditBucket
     */
    public Date getDateCreated() {
        return new Date(dateCreated);
    }

    /**
     * @return Date created in the fortress
     */
    public Date getFortressDate() {
        return new Date(fortressDate);
    }

    public String getClientRef() {
        return clientRef;
    }

    @Override
    public String toString() {
        return "AuditHeader{" +
                "id=" + id +
                ", uid='" + uid + '\'' +
                ", name='" + name + '\'' +
                // ", changes= " +auditLogs.size()+ '\'' +
                '}';
    }

    @Override
    public void bumpUpdate() {
        lastUpdated = System.currentTimeMillis();
    }

}
