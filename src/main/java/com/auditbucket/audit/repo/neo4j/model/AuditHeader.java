package com.auditbucket.audit.repo.neo4j.model;

import com.auditbucket.audit.bean.AuditHeaderInputBean;
import com.auditbucket.audit.model.IAuditHeader;
import com.auditbucket.audit.model.IAuditWhen;
import com.auditbucket.audit.model.IDocumentType;
import com.auditbucket.audit.model.ITagValue;
import com.auditbucket.registration.model.IFortress;
import com.auditbucket.registration.model.IFortressUser;
import com.auditbucket.registration.repo.neo4j.model.Fortress;
import com.auditbucket.registration.repo.neo4j.model.FortressUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User: mike
 * Date: 14/04/13
 * Time: 10:56 AM
 */
@NodeEntity(useShortNames = true)
public class AuditHeader implements IAuditHeader {

    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUser.class, type = "created", direction = Direction.INCOMING, enforceTargetType = true)
    private FortressUser createdBy;

    @RelatedTo(elementClass = FortressUser.class, type = "lastChanged", direction = Direction.OUTGOING)
    private FortressUser lastWho;

    @RelatedTo(elementClass = Fortress.class, type = "audit", direction = Direction.INCOMING)
    @Fetch
    private Fortress fortress;

    @RelatedTo(type = "classifies", direction = Direction.INCOMING)
    @Fetch
    private DocumentType documentType;

    @RelatedToVia(elementClass = AuditTagValue.class, type = "tagValue", direction = Direction.INCOMING)
    private Set<ITagValue> tagValues;

    @RelatedToVia(elementClass = AuditWhen.class, type = "logged", direction = Direction.OUTGOING)
    private Set<IAuditWhen> auditWhen = new HashSet<IAuditWhen>();

    public static final String UUID_KEY = "auditKey";

    @Indexed(indexName = UUID_KEY, unique = true)
    private String auditKey;

    @Indexed(indexName = "callerRef")
    private String name;
    private long dateCreated;

    //private String documentType;
    private String callerRef;

    private long fortressDate;
    long lastUpdated = 0;

    @Indexed(indexName = "searchKey")
    String searchKey = null;

    AuditHeader() {
        auditKey = UUID.randomUUID().toString();
        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.dateCreated = now.toDate().getTime();
        this.lastUpdated = dateCreated;
    }

    public AuditHeader(@NotEmpty IFortressUser createdBy, @NotEmpty AuditHeaderInputBean auditInput, @NotEmpty IDocumentType documentType) {
        this();
        this.documentType = (DocumentType) documentType;
        callerRef = auditInput.getCallerRef();
        Date when = auditInput.getWhen();
        if (when == null)
            fortressDate = dateCreated;
        else
            fortressDate = when.getTime();

        this.createdBy = (FortressUser) createdBy;
        this.lastWho = (FortressUser) createdBy;
        this.fortress = (Fortress) createdBy.getFortress();
        String docType = (documentType != null ? documentType.getName().toLowerCase() : "");
        this.name = (callerRef == null ? docType : (docType + "." + callerRef).toLowerCase());


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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public IFortress getFortress() {
        return fortress;
    }

    @Override
    @JsonIgnore
    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDocumentType() {
        return documentType.getName();
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonIgnore
    public Set<IAuditWhen> getAuditLogs() {
        return auditWhen;
    }

    @JsonIgnore
    public void setSearchKey(String parentKey) {
        this.searchKey = parentKey;
    }

    @JsonIgnore
    public String getSearchKey() {
        return this.searchKey;
    }


    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCallerRef() {
        return this.callerRef;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<ITagValue> getTagValues() {
        return tagValues;
    }

    public void setTagValues(Set<ITagValue> tagValues) {
        this.tagValues = tagValues;
    }
}
