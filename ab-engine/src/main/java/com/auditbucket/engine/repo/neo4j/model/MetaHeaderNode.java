/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.auditbucket.search.model.MetaSearchSchema;
import com.auditbucket.track.bean.MetaInputBean;
import com.auditbucket.track.model.DocumentType;
import com.auditbucket.track.model.MetaHeader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.Date;
import java.util.TimeZone;


/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 10:56 AM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("MetaHeader")
public class MetaHeaderNode implements MetaHeader {

    @Indexed
    private String metaKey;

    @RelatedTo(elementClass = FortressNode.class, type = "TRACKS", direction = Direction.INCOMING)
    @Fetch
    private FortressNode fortress;

    private String documentType;

    @Indexed(unique = true)
    private String callerKeyRef;

    @Indexed
    private String callerRef;

    private long dateCreated = 0;

    private String event = null; // should only be set if this is an immutable header and no log events will be recorded

    private long lastUpdated = 0;

    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUserNode.class, type = "CREATED_BY", direction = Direction.OUTGOING, enforceTargetType = true)
    private FortressUserNode createdBy;

    @RelatedTo(elementClass = FortressUserNode.class, type = "LASTCHANGED_BY", direction = Direction.OUTGOING)
    private FortressUserNode lastWho;

    public static final String UUID_KEY = "metaKey";

    @Indexed
    private String name;

    private String description;

    private long fortressDate;

    @Indexed
    private String searchKey = null;

    private boolean searchSuppressed;
    private String indexName;

    MetaHeaderNode() {

        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.dateCreated = now.toDate().getTime();
        this.lastUpdated = dateCreated;
    }

    public MetaHeaderNode(String uniqueKey, @NotEmpty FortressUser createdBy, @NotEmpty MetaInputBean metaInput, @NotEmpty DocumentType documentType) throws DatagioException {
        this();
        metaKey = uniqueKey;
        this.fortress = (FortressNode) createdBy.getFortress();
        this.documentType = (documentType != null ? documentType.getName().toLowerCase() : "");
        callerRef = metaInput.getCallerRef();
        //if ( callerRef!=null )
        callerKeyRef = this.fortress.getId() + "." + documentType.getId() + "." + (callerRef != null ? callerRef : metaKey);

        if ( name ==null )
            this.name = (callerRef == null ? this.documentType : (this.documentType + "." + callerRef));
        this.description = metaInput.getDescription();


        indexName = MetaSearchSchema.parseIndex(this.fortress);

        Date when = metaInput.getWhen();

        if (when == null)
            fortressDate = new DateTime(dateCreated, DateTimeZone.forTimeZone(TimeZone.getTimeZone(this.fortress.getTimeZone()))).getMillis();
        else
            fortressDate = when.getTime();

        lastUpdated = fortressDate;

        this.createdBy = (FortressUserNode) createdBy;
        this.lastWho = (FortressUserNode) createdBy;
        this.event = metaInput.getEvent();

        this.suppressSearch(metaInput.isSearchSuppressed());

    }

    public Long getId() {
        return id;
    }

    @Override
    public String getMetaKey() {
        return metaKey;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Fortress getFortress() {
        return fortress;
    }

    @JsonIgnore
    public String getCallerKeyRef() {
        return this.callerKeyRef;
    }

    @Override
    @JsonIgnore
    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * returns lower case representation of the documentType.name
     */
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDocumentType() {
        return documentType;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public FortressUser getLastUser() {
        return lastWho;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUser(FortressUser user) {
        lastWho = (FortressUserNode) user;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public FortressUser getCreatedBy() {
        return createdBy;
    }


    /**
     * should only be set if this is an immutable header and no log events will ever be recorded
     *
     * @return event that created this meta-header
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getEvent() {
        return event;
    }

    @Override
    @JsonIgnore
    public String getIndexName() {
        return indexName;
    }

    @Override
    public String toString() {
        return "MetaHeaderNode{" +
                "id=" + id +
                ", metaKey='" + metaKey + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public void bumpUpdate() {
        lastUpdated = new DateTime().toDateTime(DateTimeZone.UTC).toDateTime().getMillis();
    }

    /**
     * if set to true, then this change will not be indexed in the search engine
     * even if the fortress allows it
     *
     * @param searchSuppressed boolean
     */
    public void suppressSearch(boolean searchSuppressed) {
        this.searchSuppressed = searchSuppressed;

    }

    public boolean isSearchSuppressed() {
        return searchSuppressed;
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
        return this.callerRef;
    }

    @Override
    public long getWhenCreated() {
        return dateCreated;
    }

    @Override
    @JsonIgnore
    public DateTime getFortressDateCreated() {
        return new DateTime(fortressDate, DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortress.getTimeZone())));
    }

    @Override
    public void setMetaKey(String o) {
        this.metaKey = o;

    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetaHeaderNode)) return false;

        MetaHeaderNode that = (MetaHeaderNode) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}