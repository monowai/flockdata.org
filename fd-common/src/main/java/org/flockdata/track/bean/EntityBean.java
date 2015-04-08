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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.registration.bean.FortressResultBean;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.model.Entity;
import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * User: mike
 * Date: 17/11/14
 * Time: 8:47 AM
 */
public class EntityBean implements Serializable {

    private Long id ;
    private String searchKey;
    private String metaKey;
    private String fortressCode;
    private String callerRef;
    private String documentType;
    private long whenCreated;
    private String indexName;
    private boolean searchSuppressed;
    private String name;
    private String description;
    private FortressResultBean fortress;
    private DateTime fortressDateCreated;
    private String event;
    private String lastUser;
    private String createdUser;

    EntityBean (){

    }
    public EntityBean(Fortress fortress, Entity entity){
        this (entity);
        if ( indexName == null && fortress !=null )
            indexName = fortress.getIndexName();
    }
    public EntityBean(Entity entity){
        this();
        if ( entity != null ) {
            this.id = entity.getId();
            this.searchKey = entity.getSearchKey();
            this.metaKey = entity.getMetaKey();
            documentType = entity.getDocumentType();
            callerRef = entity.getCallerRef();
            whenCreated = entity.getWhenCreated();
            if ( entity.getFortress()!=null )
                indexName = entity.getFortress().getIndexName();

            description = entity.getDescription();
            searchSuppressed = entity.isSearchSuppressed();
            name = entity.getName();
            fortress = new FortressResultBean(entity.getFortress());
            event = entity.getEvent();
            fortressDateCreated = entity.getFortressDateCreated();
            if (entity.getLastUser()!=null )
                lastUser = entity.getLastUser().getCode();
            if ( createdUser !=null )
                createdUser= entity.getCreatedBy().getCode();
        }
    }

    public String getDocumentType() {
        return documentType;
    }

    void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public long getWhenCreated() {
        return whenCreated;
    }

    void setWhenCreated(long whenCreated) {
        this.whenCreated = whenCreated;
    }
    public String getMetaKey() {
        return metaKey;
    }

    void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    public String getCallerRef() {
        return callerRef;
    }

    void setCallerRef(String callerRef) {
        this.callerRef = callerRef;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSearchSuppressed() {
        return searchSuppressed;
    }

    public String getName() {
        return name;
    }

    public FortressResultBean getFortress() {
        return fortress;
    }

    @JsonIgnore
    public DateTime getFortressDateCreated() {
        return fortressDateCreated;
    }

    public String getEvent() {
        return event;
    }

    public String getLastUser() {
        return lastUser;
    }

    public String getCreatedUser() {
        return createdUser;
    }

    @JsonIgnore
    /**
     * Primary key of the node in the db. This should not be relied upon outside of
     * fd-engine and the caller should instead use their own callerRef or the metaKey
     *
     */
    public Long getId() {
        return id;
    }

    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityBean)) return false;

        EntityBean that = (EntityBean) o;

        if (callerRef != null ? !callerRef.equals(that.callerRef) : that.callerRef != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        if (metaKey != null ? !metaKey.equals(that.metaKey) : that.metaKey != null) return false;
        if (searchKey != null ? !searchKey.equals(that.searchKey) : that.searchKey != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (searchKey != null ? searchKey.hashCode() : 0);
        result = 31 * result + (metaKey != null ? metaKey.hashCode() : 0);
        result = 31 * result + (callerRef != null ? callerRef.hashCode() : 0);
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EntityBean{" +
                "metaKey='" + metaKey + '\'' +
                ", indexName='" + indexName + '\'' +
                '}';
    }
}
