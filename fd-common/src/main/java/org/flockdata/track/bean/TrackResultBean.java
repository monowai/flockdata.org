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

package org.flockdata.track.bean;

import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Since: 11/05/13
 */
public class TrackResultBean {
    private String serviceMessage;
    private LogResultBean logResult;
    private ContentInputBean contentInput;


    private EntityBean entityBean;
    private Entity entity;
    private Collection<EntityTag> tags;
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

    /**
     * Entity is only used internally by fd-engine. it can not be serialized as JSON
     * Callers should rely on entityResultBean
     *
     * @param entity  internal node
     * @param entityInputBean user supplied content to create entity
     */
    public TrackResultBean(Entity entity, EntityInputBean entityInputBean) {
        this.entity = entity;
        this.entityBean = new EntityBean(entity);
        this.entityInputBean = entityInputBean;
    }

    public TrackResultBean(Entity entity) {
        this.entityBean = new EntityBean(entity);
        this.entity = entity;

    }

    public EntityBean getEntityBean() {
        return entityBean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackResultBean)) return false;

        TrackResultBean that = (TrackResultBean) o;

        if (entityInputBean != null ? !entityInputBean.equals(that.entityInputBean) : that.entityInputBean != null)
            return false;
        if (contentInput != null ? !contentInput.equals(that.contentInput) : that.contentInput!= null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = entityInputBean != null ? entityInputBean.hashCode() : 0;
        result = 31 * result + (contentInput != null ? contentInput.hashCode() : 0);
        return result;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public void setServiceMessage(String serviceMessage) {
        this.serviceMessage = serviceMessage;
    }

    @JsonIgnore
    /**
     * @deprecated use getEntityBean
     */
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

    public void setTags(Collection<EntityTag> tags) {
        this.tags = tags;
    }

    @JsonIgnore
    /**
     * Only used when creating  relationships for the purpose of search
     * that bypass the graph
     */
    public Collection<EntityTag> getTags() {
        return tags;
    }

    @JsonIgnore
    public ContentInputBean getContentInput() {
        return contentInput;
    }

    public void setContentInput(ContentInputBean contentInputBean) {
        this.contentInput = contentInputBean;
    }

    @JsonIgnore
    public EntityInputBean getEntityInputBean() {
        return entityInputBean;
    }

    public boolean processLog() {
        return getContentInput() != null && contentInput.getStatus() != ContentInputBean.LogStatus.IGNORE;
    }
}
