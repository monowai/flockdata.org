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

package org.flockdata.track.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.flockdata.data.Entity;
import org.flockdata.data.Fortress;
import org.flockdata.data.FortressUser;
import org.flockdata.data.Segment;
import org.flockdata.registration.FortressResultBean;
import org.flockdata.registration.FortressUserResult;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author mholdsworth
 * @since 17/11/2014
 * @tag Contract, Track, Entity
 */
public class EntityResultBean implements Serializable, Entity {

    private String searchKey;
    private String key;
    private String code;
    private String type;
    private String indexName;
    private boolean searchSuppressed;
    private String name;
    private long dateCreated;
    private Date dateUpdated;
    private String event;
    private FortressUserResult lastUser;
    private FortressUserResult createdBy;
    private FortressResultBean fortress;
    private SegmentResultBean segment;
    private Integer search=0;
    private Map<String, Object> props;

    EntityResultBean() {

    }

//    public EntityResultBean(Fortress fortress, EntityInterface entity) {
//        this(entity);
//        if (indexName == null && fortress != null)
//            indexName = fortress.getRootIndex();
//    }

    public EntityResultBean(Entity entity) {
        this();
        if (entity != null) {
            this.props = entity.getProperties();
            this.searchKey = entity.getSearchKey();
            this.key = entity.getKey();

            type = entity.getType();
            code = entity.getCode();
            dateCreated = entity.getDateCreated();
            segment = new SegmentResultBean(entity.getSegment());
            indexName = entity.getSegment().getFortress().getRootIndex();
            this.search = entity.getSearch();
            // Description is recorded in the search document, not the graph
            searchSuppressed = entity.isSearchSuppressed();
            name = entity.getName();

            fortress = new FortressResultBean(entity.getSegment().getFortress());

            event = entity.getEvent();
            if (entity.getFortressCreatedTz() != null)
                dateCreated = entity.getFortressCreatedTz().toDate().getTime();
            if (entity.getFortressUpdatedTz() != null)
                dateUpdated = entity.getFortressUpdatedTz().toDate();
            if (entity.getLastUser() != null) {
                lastUser = new FortressUserResult(entity.getLastUser());
            }
            if (entity.getCreatedBy() != null)
                createdBy = new FortressUserResult(entity.getCreatedBy());
            if (lastUser == null)
                lastUser = createdBy; // This is as much as we can assume

        }
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public Long getId() {
        return null;
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

    public Fortress getFortress() {
        return fortress;
    }

    @Override
    public Segment getSegment() {
        return segment;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public DateTime getFortressCreatedTz() {
        return new DateTime(dateCreated);
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public boolean isNewEntity() {
        return false;
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public boolean isNoLogs() {
        return false;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public String getEvent() {
        return event;
    }

    public FortressUser getLastUser() {
        return lastUser;
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public Long getLastUpdate() {
        if ( dateUpdated != null)
            return dateUpdated.getTime();
        return null;
    }

    @Override
    public FortressUser getCreatedBy() {
        return createdBy;
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public Object getProperty(String key) {
        return null;
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public Map<String, Object> getProperties() {
        return null;
    }

    public Integer getSearch() {
        return search;
    }

    @Override
    @JsonIgnore // Satisfies the interface but is not part of a serializable contract
    public DateTime getFortressUpdatedTz() {
        return null;
    }

    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityResultBean)) return false;

        EntityResultBean that = (EntityResultBean) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (indexName != null ? !indexName.equals(that.indexName) : that.indexName != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return !(searchKey != null ? !searchKey.equals(that.searchKey) : that.searchKey != null);

    }

    @Override
    public int hashCode() {
        int result = (searchKey != null ? searchKey.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
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
