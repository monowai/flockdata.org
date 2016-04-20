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
import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.registration.FortressResultBean;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Map;

/**
 * User: mike
 * Date: 17/11/14
 * Time: 8:47 AM
 */
public class EntityBean implements Serializable {

    private Long id;
    private String searchKey;
    private String key;
    private String fortressCode;
    private String code;
    private String documentType;
    private long whenCreated;
    private String indexName;
    private boolean searchSuppressed;
    private String name;
    private FortressResultBean fortress;
    private DateTime fortressDateCreated;
    private DateTime fortressDateUpdated;
    private String event;
    private String lastUser;
    private String createdUser;
    private Integer search;
    private Map<String, Object> props;

    EntityBean() {

    }

    public EntityBean(Fortress fortress, Entity entity) {
        this(entity);
        if (indexName == null && fortress != null)
            indexName = fortress.getRootIndex();
    }

    public EntityBean(Entity entity) {
        this();
        if (entity != null) {
            this.id = entity.getId();
            this.props = entity.getProperties();
            this.searchKey = entity.getSearchKey();
            this.key = entity.getKey();
            documentType = entity.getType();
            code = entity.getCode();
            whenCreated = entity.getDateCreated();
            indexName = entity.getSegment().getFortress().getRootIndex();
            this.search = entity.getSearch();
            // Description is recorded in the search document, not the graph
            //description = entity.getDescription();
            searchSuppressed = entity.isSearchSuppressed();
            name = entity.getName();

            fortress = new FortressResultBean(entity.getSegment().getFortress());

            event = entity.getEvent();
            fortressDateCreated = entity.getFortressCreatedTz();
            fortressDateUpdated = entity.getFortressUpdatedTz();
            if (entity.getLastUser() != null) {
                lastUser = entity.getLastUser().getCode();
            }
            if (entity.getCreatedBy() != null)
                createdUser = entity.getCreatedBy().getCode();
            if ( lastUser == null )
                lastUser=createdUser ; // This is as much as we can assume

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

    public String getKey() {
        return key;
    }

    void setKey(String key) {
        this.key = key;
    }

    public String getCode() {
        return code;
    }

    void setCode(String code) {
        this.code = code;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
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

    @JsonIgnore
    public DateTime getFortressDateUpdated() {
        return fortressDateUpdated;
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

    public Integer getSearch() {
        return search;
    }

    @JsonIgnore
    /**
     * Primary key of the node in the db. This should not be relied upon outside of
     * fd-engine and the caller should instead use their own code or the key
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

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (documentType != null ? !documentType.equals(that.documentType) : that.documentType != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return !(searchKey != null ? !searchKey.equals(that.searchKey) : that.searchKey != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (searchKey != null ? searchKey.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (documentType != null ? documentType.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EntityBean{" +
                "key='" + key + '\'' +
                ", indexName='" + indexName + '\'' +
                '}';
    }

    public Map<String, Object> getProps() {
        return props;
    }
}
