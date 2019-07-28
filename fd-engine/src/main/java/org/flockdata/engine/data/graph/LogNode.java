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
import org.flockdata.data.ChangeEvent;
import org.flockdata.data.Entity;
import org.flockdata.data.FortressUser;
import org.flockdata.data.Log;
import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.track.bean.ContentInputBean;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;

/**
 * Captures properties about a track event for an entity
 *
 * @tag log, track
 */
@NodeEntity
@TypeAlias("Log")
public class LogNode implements Log {

  public static final String CREATE = "Create";
  public static final String UPDATE = "Update";

  @Transient
  private boolean mocked = false;
  @GraphId
  private Long id;
  //@Relationship(type = "CHANGED", direction = Relationship.INCOMING)
  @RelatedTo(type = "CHANGED", direction = Direction.INCOMING, enforceTargetType = true)
  @Fetch
  private FortressUserNode madeBy;
  //@Relationship(type = "AFFECTED", direction = Relationship.INCOMING)
  @RelatedTo(type = "AFFECTED", direction = Direction.INCOMING, enforceTargetType = true)
  private TxRefNode txRef;
  @RelatedToVia(type = "LOGGED", direction = Direction.INCOMING)
  private EntityLog entityLog;
  private String event;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String comment;
  private String storage;  // ENUMS are not serializable
  private String checkSum = null;
  private Double profileVersion = 1d;
  @Indexed(unique = true)
  private String logKey;
  private String contentType;
  private String fileName;
  private boolean compressed = false;
  @RelatedTo(type = "PREVIOUS_LOG", direction = Direction.OUTGOING)
  private LogNode previousLog;
  @Transient
  private StoredContent content = null;

  protected LogNode() {
    this.contentType = "json";
  }

  /**
   * Creates a Mock non-persistent node
   *
   * @param entity record log against
   */
  public LogNode(Entity entity) {
    //DAT-349 creates a mock node when storage is disabled
    this.id = entity.getId();
    this.mocked = true;
    this.madeBy = (entity.getCreatedBy() == null ? new FortressUserNode(entity.getSegment().getFortress(), null) : (FortressUserNode) entity.getCreatedBy());
    this.event = (entity.getEvent() == null ? "Create" : entity.getEvent());
    this.storage = Store.NONE.name();
  }

  public LogNode(FortressUserNode madeBy, ContentInputBean contentBean, TxRefNode txRef) {
    this();
    this.madeBy = madeBy;
    this.event = contentBean.getEvent();
    this.fileName = contentBean.getFileName();
    this.contentType = contentBean.getContentType();
    this.comment = contentBean.getComment();
    setTxRef(txRef);
  }

  public String getContentType() {
    if (contentType == null) {
      contentType = "json";
    }
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  @Override
  public String toString() {
    return "Log{" +
        "id=" + id +
        ", madeBy=" + madeBy +
        ", event=" + event +
        '}';
  }

  @Override
  @JsonIgnore
  public Long getId() {
    return id;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getChecksum() {
    return checkSum;
  }

  public void setChecksum(String checksum) {
    this.checkSum = checksum;
  }

  @Override
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public FortressUser getMadeBy() {
    return madeBy;
  }

  public void setMadeBy(FortressUserNode madeBy) {
    this.madeBy = madeBy;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @JsonIgnore
  public Log getPreviousLog() {
    return previousLog;
  }

  public void setPreviousLog(LogNode previousLog) {
    this.previousLog = previousLog;
  }

  public void setTxRef(TxRefNode txRef) {
    this.txRef = txRef;
  }

  public ChangeEvent getEvent() {
    // DAT-344
    if (event == null) {
      return null;
    }

    return new ChangeEventNode(event);
  }

  public void setEvent(ChangeEvent event) {
    // DAT-344
    //this.event = (ChangeEventNode) event;
    this.event = event.getName();
  }

  public boolean isCompressed() {
    return compressed;
  }

  public void setCompressed(Boolean compressed) {
    this.compressed = compressed;
  }

  @JsonIgnore
  public String getStorage() {
    return storage;
  }

  public void setStorage(String storage) {
    this.storage = storage;
  }

  public boolean equals(Object other) {
    return this == other || id != null
        && other instanceof LogNode
        && id.equals(((LogNode) other).id);

  }

  public int hashCode() {
    return id == null ? System.identityHashCode(this) : id.hashCode();
  }

  @JsonIgnore
  public EntityLog getEntityLog() {
    return entityLog;
  }

  public void setEntityLog(EntityLog entityLog) {
    // DAT-288 DAT-465
    // logKey assumes that an entity will have exactly one change on the FortressWhen date
    this.logKey = "" + entityLog.getEntity().getId() + "." + entityLog.getFortressWhen();
    //this.entityLog = entityLog;
  }

  public Double getProfileVersion() {
    return profileVersion;
  }

  @JsonIgnore
  public StoredContent getContent() {
    return content;
  }

  @JsonIgnore
  public void setContent(StoredContent storedContent) {
    this.content = storedContent;
    if (storedContent.getContent() != null) {
      this.profileVersion = storedContent.getContent().getVersion();
      this.checkSum = storedContent.getChecksum();
    }

  }

  /**
   * If we don't store the log for an entity, then we may still need to display
   * the data for it as resolved by fd-store.
   *
   * @return true if the log is not physically stored in the Graph
   */
  public boolean isMocked() {
    return mocked;
  }

}
