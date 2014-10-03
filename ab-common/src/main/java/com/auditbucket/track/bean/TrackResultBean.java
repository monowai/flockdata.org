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

package com.auditbucket.track.bean;

import com.auditbucket.track.model.Entity;
import com.auditbucket.track.model.TrackTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Since: 11/05/13
 */
public class TrackResultBean {
    private Long metaId = null;
    private String serviceMessage;
    private String fortressName;
    private String documentType;
    private String callerRef;
    private String metaKey;
    private LogResultBean logResult;
    private ContentInputBean contentInput;
    private Entity entity;
    private Collection<TrackTag> tags;
    private EntityInputBean entityInputBean;

    protected TrackResultBean() {
    }

    /**
     * @param serviceMessage server side error messgae to return to the caller
     */
    public TrackResultBean(String serviceMessage) {
        this();
        this.serviceMessage = serviceMessage;
    }

    private TrackResultBean(String fortressName, String documentType, String callerRef, String metaKey) {
        this();
        this.fortressName = fortressName;
        this.documentType = documentType;
        this.callerRef = callerRef;
        this.metaKey = metaKey;

    }

    public TrackResultBean(Entity input) {
        this(input.getFortress().getName(), input.getDocumentType(), input.getCallerRef(), input.getMetaKey());
        this.metaId = input.getId();
        this.entity = input;
    }

    public TrackResultBean(LogResultBean logResultBean, ContentInputBean input) {
        this.logResult = logResultBean;
        this.contentInput = input;
        this.entity = logResultBean.getEntity();
        // ToDo: Do we need these instance variables or just get straight from the entity?
        if (entity != null) {
            this.fortressName = entity.getFortress().getName();
            this.documentType = entity.getDocumentType();
            this.callerRef = entity.getCallerRef();
            this.metaKey = entity.getMetaKey();
        }
    }

    public String getFortressName() {
        return fortressName;
    }

    public String getDocumentType() {
        return documentType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCallerRef() {
        return callerRef;
    }

    public String getMetaKey() {
        if (entity != null)
            return entity.getMetaKey();
        return metaKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackResultBean)) return false;

        TrackResultBean that = (TrackResultBean) o;

        if (callerRef != null ? !callerRef.equals(that.callerRef) : that.callerRef != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        if (entity != null ? !entity.equals(that.entity) : that.entity != null) return false;
        if (entityInputBean != null ? !entityInputBean.equals(that.entityInputBean) : that.entityInputBean != null)
            return false;
        if (fortressName != null ? !fortressName.equals(that.fortressName) : that.fortressName != null) return false;
        if (metaId != null ? !metaId.equals(that.metaId) : that.metaId != null) return false;
        if (metaKey != null ? !metaKey.equals(that.metaKey) : that.metaKey != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = metaId != null ? metaId.hashCode() : 0;
        result = 31 * result + (fortressName != null ? fortressName.hashCode() : 0);
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + (callerRef != null ? callerRef.hashCode() : 0);
        result = 31 * result + (metaKey != null ? metaKey.hashCode() : 0);
        result = 31 * result + (entity != null ? entity.hashCode() : 0);
        result = 31 * result + (entityInputBean != null ? entityInputBean.hashCode() : 0);
        return result;
    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public void setServiceMessage(String serviceMessage) {
        this.serviceMessage = serviceMessage;
    }

    @JsonIgnore
    public Long getMetaId() {
        return metaId;
    }

    @JsonIgnore
    public Entity getEntity() {
        return entity;
    }

    public void setLogResult(LogResultBean logResult) {
        this.logResult = logResult;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public LogResultBean getLogResult() {
        return logResult;
    }

    boolean entityExisted = false;

    public void entityExisted() {
        this.entityExisted = true;
    }

    public boolean entityExists() {
        return entityExisted;
    }

    public void setTags(Collection<TrackTag> tags) {
        this.tags = tags;
    }

    @JsonIgnore
    /**
     * Only used when creating  relationships for the purpose of search
     * that bypass the graph
     */
    public Collection<TrackTag> getTags() {
        return tags;
    }

    @JsonIgnore
    public ContentInputBean getContentInput() {
        return contentInput;
    }

    public void setContentInput(ContentInputBean contentInputBean) {
        this.contentInput = contentInputBean;
    }

    public void setEntityInputBean(EntityInputBean entityInputBean) {
        this.entityInputBean = entityInputBean;
    }

    @JsonIgnore
    public EntityInputBean getEntityInputBean() {
        return entityInputBean;
    }

    public boolean processLog() {
        return getContentInput() != null && contentInput.getStatus() != ContentInputBean.LogStatus.IGNORE;
    }
}
