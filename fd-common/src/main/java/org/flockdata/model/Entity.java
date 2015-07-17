/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.helper.FlockException;
import org.flockdata.search.model.EntitySearchSchema;
import org.flockdata.track.bean.EntityInputBean;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

@NodeEntity(useShortNames = true)
@TypeAlias("Entity")
public class Entity implements Serializable {

    @Indexed
    private String metaKey;

    @RelatedTo(type = "TRACKS", direction = Direction.INCOMING)
    @Fetch
    private Fortress fortress;

    @Labels
    private ArrayList<String> labels = new ArrayList<>();

    @Indexed(unique = true)
    private String callerKeyRef;

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
    private Long fortressLastWhen = null;

    private long fortressCreate;

    @GraphId
    private Long id;

    @RelatedTo(type = "CREATED_BY", direction = Direction.OUTGOING, enforceTargetType = true)
    private FortressUser createdBy;

    @RelatedTo(type = "LASTCHANGED_BY", direction = Direction.OUTGOING)
    private FortressUser lastWho;

    @RelatedTo(type = "LAST_CHANGE", direction = Direction.OUTGOING)
    private Log lastChange;

    public static final String UUID_KEY = "metaKey";

    private int search = 0;

    //@Indexed
    private String searchKey = null;

    private boolean searchSuppressed;

    DynamicProperties props = new DynamicPropertiesContainer();

    @Transient
    private String indexName;

    @Transient
    boolean newEntity = false;

    Entity() {

        DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
        this.dateCreated = now.toDate().getTime();
        this.lastUpdate = dateCreated;
        labels.add("_Entity");
        labels.add("Entity");

    }

    public Entity(String uniqueKey, org.flockdata.model.Fortress fortress, @NotEmpty EntityInputBean entityInput, @NotEmpty DocumentType documentType) throws FlockException {
        this();

        assert documentType != null;
        assert fortress != null;

        labels.add(documentType.getName());
        metaKey = uniqueKey;
        this.fortress = fortress;//(FortressNode)documentType.getFortress();
        // DAT-278
        String docType = documentType.getName();
        if (docType == null)
            docType = documentType.getCode();

        if (docType == null)
            throw new RuntimeException("Unable to resolve the doc type code [" + documentType + "] for  " + entityInput);

        newEntity = true;

        docType = docType.toLowerCase();
        callerRef = entityInput.getCallerRef();
        callerKeyRef = this.fortress.getId() + "." + documentType.getId() + "." + (callerRef != null ? callerRef : metaKey);

        if (entityInput.getName() == null || entityInput.getName().equals(""))
            this.name = (callerRef == null ? docType : (docType + "." + callerRef));
        else
            this.name = entityInput.getName();

        this.description = entityInput.getDescription();

        indexName = EntitySearchSchema.parseIndex(this.fortress);

        if (entityInput.getProperties() != null && !entityInput.getProperties().isEmpty()) {
            props = new DynamicPropertiesContainer(entityInput.getProperties());
        }

        Date when = entityInput.getWhen();

        if (when == null)
            fortressCreate = new DateTime(dateCreated, DateTimeZone.forTimeZone(TimeZone.getTimeZone(this.fortress.getTimeZone()))).getMillis();
        else
            fortressCreate = new DateTime(when.getTime()).getMillis();//new DateTime( when.getTime(), DateTimeZone.forTimeZone(TimeZone.getTimeZone(entityInput.getMetaTZ()))).toDate().getTime();
        if ( entityInput.getLastChange()!=null ){
            long fWhen = entityInput.getLastChange().getTime();
            if ( fWhen!= fortressCreate )
                fortressLastWhen = fWhen;
        }

        // Content date has the last say on when the update happened
        if ( entityInput.getContent() !=null && entityInput.getContent().getWhen() !=null ){
            fortressLastWhen = entityInput.getContent().getWhen().getTime();
        }

        //lastUpdate = 0l;
        if (entityInput.isMetaOnly())
            this.event = entityInput.getEvent();
        this.suppressSearch(entityInput.isSearchSuppressed());

    }

    public Entity(String guid, org.flockdata.model.Fortress fortress, EntityInputBean mib, DocumentType doc, org.flockdata.model.FortressUser user) throws FlockException {
        this(guid, fortress, mib, doc);
        setCreatedBy(user);
    }

    public Long getId() {
        return id;
    }

    public String getMetaKey() {
        return metaKey;
    }

    public Fortress getFortress() {
        return fortress;
    }

    public String getCallerKeyRef() {
        return this.callerKeyRef;
    }

    public String getName() {
        return name;
    }

    /**
     * returns lower case representation of the documentType.name
     */
    @JsonIgnore
    public String getType() {
        // DAT-278
        for (String label : labels) {
            if (!label.equalsIgnoreCase("_Entity") && !label.equalsIgnoreCase("Entity"))
                return label;
        }
        return null;
    }

    public org.flockdata.model.FortressUser getLastUser() {
        return lastWho;
    }

    public Long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUser(org.flockdata.model.FortressUser user) {
        lastWho = user;
    }

    public FortressUser getCreatedBy() {
        return createdBy;
    }

    public Object getProperty(String name) {
        return props.getProperty(name);
    }

    public Map<String, Object> getProperties() {
        return props.asMap();
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

    public void setLastChange(Log newChange) {
        this.lastChange = newChange;
    }

    public void setFortressLastWhen(Long fortressWhen) {
        this.fortressLastWhen = fortressWhen;
    }

    @Override
    public String toString() {
        return "EntityNode{" +
                "id=" + id +
                ", metaKey='" + metaKey + '\'' +
                ", name='" + name + '\'' +
                ", fortress='" + fortress + '\'' +
                '}';
    }

    public void bumpUpdate() {
        if (id != null)
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

    public void setSearchKey(String searchKey) {
        // By default the searchkey is the callerRef. Let's save disk space
        if ( searchKey!=null && searchKey.equals(callerRef))
            this.searchKey = null;
        else
            this.searchKey = searchKey;
    }

    public String getSearchKey() {
//        if ( search  == 0) // No search reply received so searchKey is not yet valid
//            return null;
        return (searchKey == null ? callerRef: searchKey);

    }

    public String getCallerRef() {
        return this.callerRef;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    @JsonIgnore
    public DateTime getFortressCreatedTz() {
        return new DateTime(fortressCreate, DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortress.getTimeZone())));
    }

    @JsonIgnore     // Don't persist ov
    public DateTime getFortressUpdatedTz() {
        if ( fortressLastWhen == null )
            return null;
        return new DateTime(fortressLastWhen, DateTimeZone.forTimeZone(TimeZone.getTimeZone(fortress.getTimeZone())));
    }

    public String getDescription() {
        return description;
    }

    public void bumpSearch() {
        search++; // Increases the search count of the entity.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;

        Entity that = (Entity) o;

        return !(metaKey != null ? !metaKey.equals(that.metaKey) : that.metaKey != null);

    }

    @Override
    public int hashCode() {
        return metaKey != null ? metaKey.hashCode() : 0;
    }

    public void setCreatedBy(FortressUser createdBy) {
        this.createdBy = createdBy;
    }

    public Log getLastChange() {
        return lastChange;
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public void setNew() {
        setNewEntity(true);
    }

    public void setNewEntity(boolean status) {
        this.newEntity = status;
    }

    public boolean setProperties(Map<String, Object> properties) {
        boolean modified = false;
        for (String s : properties.keySet()) {
            if (props.hasProperty(s)) {
                if (props.getProperty(s) != properties.get(s))
                    modified = true;
            } else
                modified = true;


        }
        if (modified)
            props = new DynamicPropertiesContainer(properties);
        return modified;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNewEntity() {
        return newEntity;
    }
}