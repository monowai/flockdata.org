/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.track.bean;

import org.flockdata.model.Entity;
import org.flockdata.model.Fortress;
import org.flockdata.registration.FortressResultBean;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author mholdsworth
 * @since 17/11/2014
 */
public class EntityBean implements Serializable {

    private String searchKey;
    private String key;
    private String code;
    private String documentType;
    private String indexName;
    private boolean searchSuppressed;
    private String name;
    private FortressResultBean fortress;
    private Date dateCreated;
    private Date dateUpdated;
    private String event;
    private String lastUser;
    private String createdUser;
    private Integer search=0;
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
            this.props = entity.getProperties();
            this.searchKey = entity.getSearchKey();
            this.key = entity.getKey();
            documentType = entity.getType();
            code = entity.getCode();
//            dateCreated = new DateTime(entity.getDateCreated());
            indexName = entity.getSegment().getFortress().getRootIndex();
            this.search = entity.getSearch();
            // Description is recorded in the search document, not the graph
            searchSuppressed = entity.isSearchSuppressed();
            name = entity.getName();

            fortress = new FortressResultBean(entity.getSegment().getFortress());

            event = entity.getEvent();
            if (entity.getFortressCreatedTz() != null)
                dateCreated = entity.getFortressCreatedTz().toDate();
            if (entity.getFortressUpdatedTz() != null)
                dateUpdated = entity.getFortressUpdatedTz().toDate();
            if (entity.getLastUser() != null) {
                lastUser = entity.getLastUser().getCode();
            }
            if (entity.getCreatedBy() != null)
                createdUser = entity.getCreatedBy().getCode();
            if (lastUser == null)
                lastUser = createdUser; // This is as much as we can assume

        }
    }

    public String getDocumentType() {
        return documentType;
    }

    void setDocumentType(String documentType) {
        this.documentType = documentType;
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

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
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
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return !(searchKey != null ? !searchKey.equals(that.searchKey) : that.searchKey != null);

    }

    @Override
    public int hashCode() {
        int result = (searchKey != null ? searchKey.hashCode() : 0);
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
