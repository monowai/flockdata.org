/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.track.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.company.model.FortressNode;
import org.flockdata.company.model.FortressUserNode;
import org.flockdata.helper.FlockException;
import org.flockdata.registration.model.Fortress;
import org.flockdata.registration.model.FortressUser;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.DocumentType;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.Log;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;


/**
 * User: Mike Holdsworth
 * Date: 14/04/13
 * Time: 10:56 AM
 */
@NodeEntity(useShortNames = true)
@TypeAlias("Entity")
public class EntityNode implements Entity {

    @Indexed
    private String metaKey;

    @RelatedTo(type = "TRACKS", direction = Direction.INCOMING)
    @Fetch
    private FortressNode fortress;

    @Labels
    private ArrayList<String> labels = new ArrayList<>();

    @Indexed(unique = true)
    private String callerKeyRef;

    @Indexed
    private String callerRef;

    private String name;

    private String description;

    // By the Fortress
    private long dateCreated = 0;

    // should only be set if this is an immutable entity and no log events will be recorded
    private String event = null;

    // By FlockData, set in UTC
    private long lastUpdate = 0;

    // Fortress in fortress timezone
    private long fortressLastWhen;

    private long fortressCreate;

    @GraphId
    private Long id;

    @RelatedTo(type = "CREATED_BY", direction = Direction.OUTGOING, enforceTargetType = true)
    private FortressUserNode createdBy;

    @RelatedTo(type = "LASTCHANGED_BY", direction = Direction.OUTGOING)
    private FortressUserNode lastWho;

    @RelatedTo(type = "LAST_CHANGE", direction = Direction.OUTGOING)
    private LogNode lastChange;

    public static final String UUID_KEY = "metaKey";

    private int search=0;

    //@Indexed
    private String searchKey = null;

    private boolean searchSuppressed;

    @Transient
    private String indexName;

    @Transient
    boolean isNew = false;


    EntityNode() {

        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.dateCreated = now.toDate().getTime();
        this.lastUpdate = dateCreated;
        labels.add("_Entity");
        labels.add("Entity");

    }

    public EntityNode(String uniqueKey,Fortress fortress,  @NotEmpty EntityInputBean entityInput, @NotEmpty DocumentType documentType) throws FlockException {
        this();

        assert documentType != null;
        assert fortress!=null;

        labels.add(documentType.getName());
        metaKey = uniqueKey;
        this.fortress = (FortressNode)fortress;//(FortressNode)documentType.getFortress();
        // DAT-278
        String docType = documentType.getName();
        if ( docType == null)
            docType = documentType.getCode();

        if ( docType == null )
            throw new RuntimeException("Unable to resolve the doc type code ["+documentType+"] for  "+entityInput)  ;

        isNew = true;

        docType = docType.toLowerCase();
        callerRef = entityInput.getCallerRef();
        callerKeyRef = this.fortress.getId() + "." + documentType.getId() + "." + (callerRef != null ? callerRef : metaKey);

        if (entityInput.getName() == null || entityInput.getName().equals(""))
            this.name = (callerRef == null ? docType : (docType + "." + callerRef));
        else
            this.name = entityInput.getName();

        this.description = entityInput.getDescription();

        indexName = EntitySearchSchema.parseIndex(this.fortress);

        Date when = entityInput.getWhen();

        if (when == null)
            fortressCreate = new DateTime(dateCreated, DateTimeZone.forTimeZone(TimeZone.getTimeZone(this.fortress.getTimeZone()))).getMillis();
        else
            fortressCreate = new DateTime (when.getTime()).getMillis();//new DateTime( when.getTime(), DateTimeZone.forTimeZone(TimeZone.getTimeZone(entityInput.getMetaTZ()))).toDate().getTime();

        lastUpdate = 0l;
        if ( entityInput.isMetaOnly())
            this.event = entityInput.getEvent();
        this.suppressSearch(entityInput.isSearchSuppressed());

    }

    public EntityNode(String guid, Fortress fortress, EntityInputBean mib, DocumentType doc, FortressUser user) throws FlockException {
        this(guid, fortress, mib, doc);
        setCreatedBy(user);
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
        return name;
    }

    /**
     * returns lower case representation of the documentType.name
     */
    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getDocumentType() {
        // DAT-278
        for (String label : labels) {
            if (!label.equalsIgnoreCase("_Entity") && ! label.equalsIgnoreCase("Entity"))
                return label.toLowerCase();
        }
        return null;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public FortressUser getLastUser() {
        return lastWho;
    }

    public Long getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public Long getFortressDateUpdated() {
        return fortressLastWhen;
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
     * should only be set if this is an immutable entity and no log events will ever be recorded
     *
     * @return event that created this entity
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getEvent() {
        return event;
    }

    @Override
    public void setLastChange(Log newChange) {
        this.lastChange = (LogNode) newChange;
    }

    @Override
    public void setFortressLastWhen(Long fortressWhen) {
        this.fortressLastWhen = fortressWhen;
    }

    @Override
    public String toString() {
        return "EntityNode{" +
                "id=" + id +
                ", metaKey='" + metaKey + '\'' +
                ", name='" + name + '\'' +
                ", fortress='" +fortress+ '\'' +
                '}';
    }

    @Override
    public void bumpUpdate() {
        if ( id != null )
            lastUpdate = new DateTime().toDateTime(DateTimeZone.UTC).toDateTime().getMillis();
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
    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    @JsonIgnore
    public String getSearchKey() {
        return searchKey;

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
        return new DateTime(fortressCreate, DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortress.getTimeZone())));
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void bumpSearch() {
        search++; // Increases the search count of the entity.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityNode)) return false;

        EntityNode that = (EntityNode) o;

        return !(metaKey != null ? !metaKey.equals(that.metaKey) : that.metaKey != null);

    }

    @Override
    public int hashCode() {
        return metaKey != null ? metaKey.hashCode() : 0;
    }

    public void setCreatedBy(FortressUser createdBy) {
        this.createdBy = (FortressUserNode)createdBy;
    }

    public Log getLastChange() {
        return lastChange;
    }

    public void setLastChange(LogNode lastChange) {
        this.lastChange = lastChange;
    }

    public void addLabel(String label){
        labels.add(label);
    }

    public void setNew() {
        setNew(true);
    }

    public void setNew(boolean status) {
        this.isNew = status;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
