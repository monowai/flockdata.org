/*
 *  Copyright 2012-2016 the original author or authors.
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
import org.flockdata.model.*;
import org.neo4j.graphdb.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the internal in-memory state of a request to record a change in FlockData
 * This payload is passed around services enriched and returned.
 * <p/>
 * TrackResultBean is not persisted and it's state is only guaranteed within FlockData
 * @see org.flockdata.track.bean.TrackRequestResult for user represetned results
 * <p/>
 * User: Mike Holdsworth
 * Since: 11/05/13
 */
public class TrackResultBean implements Serializable {
    private Collection<String> serviceMessages = new ArrayList<>();

    private Entity entity;        // Resolved entity
    private EntityLog currentLog; // Log that was created
    private EntityLog deletedLog; // Log that was removed in response to a cancel request

    private Collection<EntityTag> tags; // Tags connected to the entity

    private EntityInputBean entityInputBean;// User payload
    private ContentInputBean contentInput;  // User content payload

    private DocumentType documentType;
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

    /**
     * Entity is only used internally by fd-engine; it can not be serialized as JSON
     * Callers should rely on entityResultBean
     *
     * @param entity          internal node
     * @param entityInputBean user supplied content to create entity
     */
    public TrackResultBean(Fortress fortress, Entity entity, DocumentType documentType, EntityInputBean entityInputBean) {
        this(entity);
        this.entityInputBean = entityInputBean;
        this.company = fortress.getCompany();
        this.contentInput = entityInputBean.getContent();
        this.documentType = documentType;
        this.index = fortress.getRootIndex();
    }

    private TrackResultBean(Entity entity) {
        this.entity = entity;
        this.company = entity.getFortress().getCompany();
        this.newEntity = entity.isNewEntity();
    }

    public TrackResultBean(Entity entity, DocumentType documentType) {
        this(entity);
        this.documentType = documentType;
    }

    public TrackResultBean(Fortress fortress, Node entity, EntityInputBean entityInputBean) {
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
     */
    public Entity getEntity() {
        return entity;
    }

    public void setCurrentLog(EntityLog currentLog) {
        this.currentLog = currentLog;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public EntityLog getCurrentLog() {
        return currentLog;
    }

    public void setDeletedLog(EntityLog entityLog) {
        this.deletedLog = entityLog;
    }

    public EntityLog getDeletedLog() {
        return deletedLog;
    }

    boolean entityExisted = false;

    public void entityExisted() {
        this.entityExisted = true;
    }

    public boolean entityExists() {
        return entityExisted;
    }

    public void setTags(Collection<EntityTag> tags) {
        this.tags = tags;
    }

    @JsonIgnore
    /**
     * If trackSuppressed is true, then mock EntityTags are created for the purpose
     * of building a search document. This method returns those mocked entity tags
     *
     * If you want actual EntityTags physically recorded against the Entity then use the
     * EntityTagService
     *
     */
    public Collection<EntityTag> getTags() {
        return tags;
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

    public TrackResultBean setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
        return this;
    }

    @JsonIgnore
    public DocumentType getDocumentType() {
        return documentType;
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
    // Convenience function to get the Enitty key
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

    boolean logIgnored = false;

    public void setLogIgnored() {
        this.logIgnored = true;
    }

    public boolean isLogIgnored() {
        // FixMe: Suspicious about the TRACK_ONLY status. One can ignore track and write to fd-search
        return logIgnored ||
                getLogStatus() == ContentInputBean.LogStatus.IGNORE ||
                getLogStatus() == ContentInputBean.LogStatus.TRACK_ONLY;
    }

    public void setTxReference(TxRef txReference) {
        this.txReference = txReference;
    }

    public TxRef getTxReference() {
        return txReference;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @Override
    public String toString() {
        return "TrackResultBean{" +
                "entity=" + entity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackResultBean)) return false;

        TrackResultBean that = (TrackResultBean) o;

        if (entityInputBean != null ? !entityInputBean.equals(that.entityInputBean) : that.entityInputBean != null)
            return false;
        return !(contentInput != null ? !contentInput.equals(that.contentInput) : that.contentInput != null);

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
}
