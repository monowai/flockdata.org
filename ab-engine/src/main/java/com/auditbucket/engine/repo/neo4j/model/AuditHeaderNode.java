/*
 * Copyright (c) 2012-2013 "Monowai Developments Limited"
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

import com.auditbucket.audit.model.AuditHeader;
import com.auditbucket.audit.model.DocumentType;
import com.auditbucket.audit.model.AuditTag;
import com.auditbucket.bean.AuditHeaderInputBean;
import com.auditbucket.registration.model.Fortress;
import com.auditbucket.registration.model.FortressUser;
import com.auditbucket.registration.repo.neo4j.model.FortressNode;
import com.auditbucket.registration.repo.neo4j.model.FortressUserNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.*;

import java.util.*;


/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 10:56 AM
 */
@NodeEntity(useShortNames = true)
public class AuditHeaderNode implements AuditHeader {

    @Transient
    private Logger log = LoggerFactory.getLogger(AuditHeaderNode.class);

    @Indexed(indexName = UUID_KEY, unique = true)
    private String auditKey;

    @RelatedTo(elementClass = FortressNode.class, type = "audit", direction = Direction.INCOMING)
    @Fetch
    private FortressNode fortress;

    @RelatedTo(type = "classifies", direction = Direction.INCOMING)
    @Fetch
    private DocumentTypeNode documentType;

    @Indexed(indexName = "callerRef")
    private String callerRef;

    private long dateCreated;

    private String event = null; // should only be set if this is an immutable header and no log events will be recorded

    private long lastUpdated = 0;

    @GraphId
    private Long id;

    @RelatedTo(elementClass = FortressUserNode.class, type = "created", direction = Direction.INCOMING, enforceTargetType = true)
    private FortressUserNode createdBy;

    @RelatedTo(elementClass = FortressUserNode.class, type = "lastChanged", direction = Direction.OUTGOING)
    private FortressUserNode lastWho;

    @RelatedToVia(elementClass = AuditTagRelationship.class, type = "auditTag", direction = Direction.INCOMING)
    private Set<AuditTag> tagValues;

    public static final String UUID_KEY = "auditKey";

    private String name;

    private long fortressDate;

    @Indexed(indexName = "searchKey")
    private
    String searchKey = null;

    private boolean searchSuppressed;
    private String indexName;

    AuditHeaderNode() {

        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.dateCreated = now.toDate().getTime();
        this.lastUpdated = dateCreated;
    }

    public AuditHeaderNode(String uniqueKey, @NotEmpty FortressUser createdBy, @NotEmpty AuditHeaderInputBean auditInput, @NotEmpty DocumentType documentType) {
        this();
        auditKey = uniqueKey;
        this.fortress = (FortressNode) createdBy.getFortress();
        this.documentType = (DocumentTypeNode) documentType;
        String docType = (documentType != null ? getDocumentType() : "");
        this.name = (callerRef == null ? docType : (docType + "." + callerRef).toLowerCase());

        indexName = createdBy.getFortress().getCompany().getName().toLowerCase() + "." + fortress.getName().toLowerCase();
        callerRef = auditInput.getCallerRef();
        if (callerRef != null)
            callerRef = callerRef.toLowerCase();

        Date when = auditInput.getWhen();

        if (when == null)
            fortressDate = new DateTime(dateCreated, DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortress.getTimeZone()))).getMillis();
        else
            fortressDate = when.getTime();

        this.createdBy = (FortressUserNode) createdBy;
        this.lastWho = (FortressUserNode) createdBy;
        this.event = auditInput.getEvent();

        this.suppressSearch(auditInput.isSuppressSearch());

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
    public Fortress getFortress() {
        return fortress;
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
        return documentType.getName().toLowerCase();
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
        return "AuditHeaderNode{" +
                "id=" + id +
                ", auditKey='" + auditKey + '\'' +
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

    @JsonIgnore
    public Set<AuditTag> getTagValues() {
        return tagValues;
    }

    public Map<String, String> getTagMap() {
        Map<String, String> result = new HashMap<>();
        if (tagValues != null)
            for (AuditTag tagValue : tagValues) {
                result.put(tagValue.getTag().getName(), tagValue.getTagType());
            }
        return result;
    }

    @Override
    public long getWhenCreated() {
        return dateCreated;
    }

    @Override
    public void setTags(Set<AuditTag> auditTags) {
        this.tagValues = auditTags;
    }

    @Override
    @JsonIgnore
    public DateTime getFortressDateCreated() {
        return new DateTime(fortressDate, DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortress.getTimeZone())));
    }
}
