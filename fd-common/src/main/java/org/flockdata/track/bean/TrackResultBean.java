/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import org.flockdata.data.Company;
import org.flockdata.data.Document;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityLog;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Fortress;
import org.flockdata.data.TxRef;

/**
 * Represents the internal in-memory state of a request to record a change in FlockData
 * This payload is passed around services enriched and returned.
 * <p>
 * TrackResultBean is not persisted and it's state is only guaranteed within FlockData
 *
 * @author mholdsworth
 * @tag Contract, Track, Entity
 * @see org.flockdata.track.bean.TrackRequestResult for user represetned results
 * @since 11/05/2013
 */
public class TrackResultBean implements Serializable {
  private boolean entityExisted = false;
  private boolean logIgnored = false;
  private Collection<String> serviceMessages = new ArrayList<>();
  private Entity entity;        // Resolved entity
  private EntityLog currentLog; // Log that was created
  private EntityLog deletedLog; // Log that was removed in response to a cancel request
  private Collection<EntityTag> tags; // Tags connected to the entity
  private EntityInputBean entityInputBean;// User payload
  private ContentInputBean contentInput;  // User content payload
  private Document documentType;
  private String index;       // Which index is this indexed in
  private Boolean newEntity = false; // Flags that the Entity was created for the first time
  private ContentInputBean.LogStatus logStatus; // What status
  private TxRef txReference = null; // Reference used to track the transaction
  private String tenant = "";
  private Company company;

  protected TrackResultBean() {
  }

  /**
   * @param serviceMessage server side error messages to return to the caller
   */
  public TrackResultBean(String serviceMessage) {
    this();
    addServiceMessage(serviceMessage);
  }

  private TrackResultBean(Entity entity) {
    this.entity = entity;
    this.company = entity.getFortress().getCompany();
    this.newEntity = entity.isNewEntity();
  }

  /**
   * Entity is only used internally by fd-engine; it can not be serialized as JSON
   * Callers should rely on entityResultBean
   *
   * @param fortress        owner of the entity
   * @param entity          internal node
   * @param documentType    type of Entity that was tracked
   * @param entityInputBean user supplied content to create entity
   */
  public TrackResultBean(Fortress fortress, Entity entity, Document documentType, EntityInputBean entityInputBean) {
    this(entity);
    this.entityInputBean = entityInputBean;
    this.company = fortress.getCompany();
    this.contentInput = entityInputBean.getContent();
    this.documentType = documentType;
    this.index = fortress.getRootIndex();
  }

  public TrackResultBean(Entity entity, Document documentType) {
    this(entity);
    this.documentType = documentType;
  }

  public TrackResultBean(Fortress fortress, EntityInputBean entityInputBean) {
    //this.entityBean = new EntityBean(fortress, entity, null);
    //this.entity = new Entity(fortress, entity);
    this.entityInputBean = entityInputBean;
    this.contentInput = entityInputBean.getContent();
    this.index = fortress.getRootIndex();

  }

  /**
   * Error messages to return to the caller
   *
   * @return Array of messages pertaining to this track request
   */
  public Collection<String> getServiceMessages() {
    return serviceMessages;
  }

  public void addServiceMessage(String serviceMessage) {

    this.serviceMessages.add(serviceMessage);
  }

  /**
   * @return details of the created entity
   */
  public Entity getEntity() {
    return entity;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public EntityLog getCurrentLog() {
    return currentLog;
  }

  public void setCurrentLog(EntityLog currentLog) {
    this.currentLog = currentLog;
  }

  public EntityLog getDeletedLog() {
    return deletedLog;
  }

  public void setDeletedLog(EntityLog entityLog) {
    this.deletedLog = entityLog;
  }

  public void entityExisted() {
    this.entityExisted = true;
  }

  public boolean entityExists() {
    return entityExisted;
  }

  /**
   * If trackSuppressed is true, then mock EntityTags are created for the purpose
   * of building a search document. This method returns those mocked entity tags
   * <p>
   * If you want actual EntityTags physically recorded against the Entity then use the
   * EntityTagService
   *
   * @return Tags that were attached to the Entity by this request
   */
  @JsonIgnore
  public Collection<EntityTag> getTags() {
    return tags;
  }

  public void setTags(Collection<EntityTag> tags) {
    this.tags = tags;
  }

  public ContentInputBean getContentInput() {
    // ToDo: Why are we tracking input in 2 places? Tracking content having already created the entity?
    // do with the "trackLog" endpoint
    return (entityInputBean == null ? contentInput : entityInputBean.getContent());
  }

  /**
   * Content being tracked
   *
   * @param contentInputBean content provided as input to the track process
   */
  public void setContentInput(ContentInputBean contentInputBean) {
    this.contentInput = contentInputBean;
  }

  /**
   * EntityInput information provided when the track call was made
   *
   * @return callers input data
   */
  public EntityInputBean getEntityInputBean() {
    return entityInputBean;
  }

  /**
   * @return true if this log should be processed by the search service
   */
  public boolean processLog() {
    return (getContentInput() != null && logStatus != ContentInputBean.LogStatus.IGNORE);
  }

  @JsonIgnore
  public Document getDocumentType() {
    return documentType;
  }

  public TrackResultBean setDocumentType(Document documentType) {
    this.documentType = documentType;
    return this;
  }

  public String getIndex() {
    return index;
  }

  public TrackResultBean setNewEntity() {
    newEntity = true;
    return this;
  }

  /**
   * @return is this a new Entity for FlockData?
   */
  public Boolean isNewEntity() {
    return newEntity;
  }

  @JsonIgnore
  // Convenience function to get the Entity key
  public String getKey() {
    return entity.getKey();
  }

  public ContentInputBean.LogStatus getLogStatus() {
    return logStatus;
  }

  public TrackResultBean setLogStatus(ContentInputBean.LogStatus logStatus) {
    this.logStatus = logStatus;
    return this;
  }

  public void setLogIgnored() {
    this.logIgnored = true;
  }

  public boolean isLogIgnored() {
    // FixMe: Suspicious about the TRACK_ONLY status. One can ignore track and write to fd-search
    return logIgnored ||
        getLogStatus() == ContentInputBean.LogStatus.IGNORE ||
        getLogStatus() == ContentInputBean.LogStatus.TRACK_ONLY;
  }

  public TxRef getTxReference() {
    return txReference;
  }

  public void setTxReference(TxRef txReference) {
    this.txReference = txReference;
  }

  public String getTenant() {
    return tenant;
  }

  public void setTenant(String tenant) {
    this.tenant = tenant;
  }

  @Override
  public int hashCode() {
    int result = entityInputBean != null ? entityInputBean.hashCode() : 0;
    result = 31 * result + (contentInput != null ? contentInput.hashCode() : 0);
    return result;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  @Override
  public String toString() {
    return "TrackResultBean{" +
        "entity=" + entity +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TrackResultBean)) {
      return false;
    }

    TrackResultBean that = (TrackResultBean) o;

    if (entityInputBean != null ? !entityInputBean.equals(that.entityInputBean) : that.entityInputBean != null) {
      return false;
    }
    return !(contentInput != null ? !contentInput.equals(that.contentInput) : that.contentInput != null);

  }
}
