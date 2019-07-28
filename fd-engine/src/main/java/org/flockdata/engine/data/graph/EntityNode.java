/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.engine.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.FortressUser;
import org.flockdata.data.Log;
import org.flockdata.data.Segment;
import org.flockdata.helper.FlockException;
import org.flockdata.track.EntityHelper;
import org.flockdata.track.bean.EntityInputBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

/**
 * @tag Entity, Node, Segment
 */
@NodeEntity
@TypeAlias("Entity")
public class EntityNode implements Serializable, Entity {

  public static final String UUID_KEY = "key";
  DynamicProperties props = new DynamicPropertiesContainer();
  @Transient
  boolean newEntity = false;
  @Indexed
  private String key;
  //@Relationship(type = "TRACKS", direction = Relationship.INCOMING)
  @RelatedTo(type = "TRACKS", direction = Direction.INCOMING)
  @Fetch
  private FortressSegmentNode segment;
  @Labels
  private ArrayList<String> labels = new ArrayList<>();
  @Indexed(unique = true)
  private String extKey;   // Calculated field defining a unique external key
  @Indexed
  private String code;
  private String name;
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

  //@Relationship(type = "LOGGED")
  //Set<EntityLog> logs = new HashSet<>();
  //@Relationship(type = "CREATED_BY", direction = Relationship.OUTGOING)
  @RelatedTo(type = "CREATED_BY", direction = Direction.OUTGOING, enforceTargetType = true)
  private FortressUserNode createdBy;
  //@Relationship(type = "LASTCHANGED_BY", direction = Relationship.OUTGOING)
  @RelatedTo(type = "LASTCHANGED_BY", direction = Direction.OUTGOING)
  private FortressUserNode lastWho;
  //@Relationship(type = "LAST_CHANGE", direction = Relationship.OUTGOING)
  @RelatedTo(type = "LAST_CHANGE", direction = Direction.OUTGOING)
  private LogNode lastChange;
  private String searchKey = null;
  private boolean searchSuppressed;
  private boolean noLogs = false;
  @Transient
  private String indexName;
  private Integer search = 0;

  public EntityNode(String key, FortressNode fortress, EntityInputBean eib, Document doc) throws FlockException {
    this(key, fortress.getDefaultSegment(), eib, doc);
  }

  EntityNode() {

    DateTime now = new DateTime().toDateTime(DateTimeZone.UTC);
    this.dateCreated = now.toDate().getTime();
    this.lastUpdate = dateCreated;
    labels.add("_Entity");
    labels.add("Entity");

  }

  public EntityNode(String key, Segment segment, EntityInputBean entityInput, Document documentType) throws FlockException {
    this();

    assert documentType != null;
    assert segment != null;

    labels.add(documentType.getName());
    this.key = key;
    this.noLogs = entityInput.isEntityOnly();
    this.segment = (FortressSegmentNode) segment;
    // DAT-278
    String docType = documentType.getName();
    if (docType == null) {
      docType = documentType.getCode();
    }

    if (docType == null) {
      throw new RuntimeException("Unable to resolve the doc type code [" + documentType + "] for  " + entityInput);
    }

    newEntity = true;

    docType = docType.toLowerCase();
    code = entityInput.getCode();
    extKey = EntityHelper.parseKey(this.segment.getFortress().getId(), documentType.getId(), (code != null ? code : this.key));
    //extKey = this.fortress.getId() + "." + documentType.getId() + "." + (code != null ? code : key);

    if (entityInput.getName() == null || entityInput.getName().equals("")) {
      this.name = (code == null ? docType : (docType + "." + code));
    } else {
      this.name = entityInput.getName();
    }

    if (entityInput.getProperties() != null && !entityInput.getProperties().isEmpty()) {
      props = new DynamicPropertiesContainer(entityInput.getProperties());
    }

    Date when = entityInput.getWhen();

    if (when == null) {
      fortressCreate = new DateTime(dateCreated, DateTimeZone.forTimeZone(TimeZone.getTimeZone(this.segment.getFortress().getTimeZone()))).getMillis();
    } else {
      fortressCreate = new DateTime(when.getTime()).getMillis();//new DateTime( when.getTime(), DateTimeZone.forTimeZone(TimeZone.getTimeZone(entityInput.getMetaTZ()))).toDate().getTime();
    }
    if (entityInput.getLastChange() != null) {
      long fWhen = entityInput.getLastChange().getTime();
      if (fWhen != fortressCreate) {
        fortressLastWhen = fWhen;
      }
    }

    // Content date has the last say on when the update happened
    if (entityInput.getContent() != null && entityInput.getContent().getWhen() != null) {
      fortressLastWhen = entityInput.getContent().getWhen().getTime();
    }

    //lastUpdate = 0l;
    if (entityInput.isEntityOnly()) {
      this.event = entityInput.getEvent();
    }
    this.suppressSearch(entityInput.isSearchSuppressed());

  }

  public EntityNode(String guid, Segment segment, EntityInputBean mib, Document doc, FortressUser user) throws FlockException {
    this(guid, segment, mib, doc);
    setCreatedBy(user);
  }

  public Integer getSearch() {
    return search;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getKey() {
    return key;
  }

  public Segment getSegment() {
    return segment;
  }

  public void setSegment(Segment segment) {
    this.segment = (FortressSegmentNode) segment;
  }

  public String getExtKey() {
    return this.extKey;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  @JsonIgnore
  public String getType() {
    // DAT-278
    return EntityHelper.getLabel(labels);
  }

  @Override
  public FortressUserNode getLastUser() {
    return lastWho;
  }

  public void setLastUser(FortressUserNode user) {
    lastWho = user;
  }

  @Override
  public Long getLastUpdate() {
    return lastUpdate;
  }

  @Override
  public FortressUserNode getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(FortressUser createdBy) {
    this.createdBy = (FortressUserNode) createdBy;
  }

  @Override
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

  public void setFortressLastWhen(Long fortressWhen) {
    this.fortressLastWhen = fortressWhen;
  }

  @Override
  public String toString() {
    return "EntityNode{" +
        "id=" + id +
        ", key='" + key + '\'' +
        ", name='" + name +
        '}';
  }

  public void bumpUpdate() {
    if (id != null) {
      lastUpdate = new DateTime().toDateTime(DateTimeZone.UTC).toDateTime().getMillis();
    }
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

  @Override
  public boolean isSearchSuppressed() {
    return searchSuppressed;
  }

  @Override
  public String getSearchKey() {
//        if ( search  == 0) // No search reply received so searchKey is not yet valid
//            return null;
    return (searchKey == null ? code : searchKey);

  }

  public void setSearchKey(String searchKey) {
    // By default the searchkey is the code. Let's save disk space
    if (searchKey != null && searchKey.equals(code)) {
      this.searchKey = null;
    } else {
      this.searchKey = searchKey;
    }
  }

  @Override
  public String getCode() {
    return this.code;
  }

  @Override
  public long getDateCreated() {
    return dateCreated;
  }

  @Override
  @JsonIgnore
  public DateTime getFortressCreatedTz() {
    return new DateTime(fortressCreate, DateTimeZone.forTimeZone(TimeZone.getTimeZone(segment.getFortress().getTimeZone())));
  }

  @JsonIgnore     // Don't persist ov
  public DateTime getFortressUpdatedTz() {
    if (fortressLastWhen == null) {
      return null;
    }
    return new DateTime(fortressLastWhen, DateTimeZone.forTimeZone(TimeZone.getTimeZone(segment.getFortress().getTimeZone())));
  }

  public void bumpSearch() {
    search++; // Increases the search count of the entity.
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityNode)) {
      return false;
    }

    EntityNode that = (EntityNode) o;

    return !(key != null ? !key.equals(that.key) : that.key != null);

  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  public Log getLastChange() {
    return lastChange;
  }

  public void setLastChange(LogNode newChange) {
    this.lastChange = newChange;
  }

  public void addLabel(String label) {
    labels.add(label);
  }

  public void setNew() {
    setNewEntity(true);
  }

  public boolean setProperties(Map<String, Object> properties) {
    boolean modified = false;
    for (String s : properties.keySet()) {
      if (props.hasProperty(s)) {
        if (props.getProperty(s) != properties.get(s)) {
          modified = true;
        }
      } else {
        modified = true;
      }


    }
    if (modified) {
      props = new DynamicPropertiesContainer(properties);
    }
    return modified;
  }

  @Override
  public boolean isNewEntity() {
    return newEntity;
  }

  public void setNewEntity(boolean status) {
    this.newEntity = status;
  }

  // Stores the EntityInputBean entityOnly value
  @Override
  public boolean isNoLogs() {
    return noLogs;
  }

  @Override
  @JsonIgnore
  public Fortress getFortress() {
    return segment.getFortress();
  }

  public EntityNode setIndexName(String indexName) {
    this.indexName = indexName;
    return this;
  }

}