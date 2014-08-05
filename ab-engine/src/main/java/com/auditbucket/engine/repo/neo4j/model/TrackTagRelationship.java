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

package com.auditbucket.engine.repo.neo4j.model;

import com.auditbucket.registration.model.Tag;
import com.auditbucket.track.model.GeoData;
import com.auditbucket.track.model.MetaHeader;
import com.auditbucket.track.model.TrackTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.neo4j.graphdb.Relationship;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:59 PM
 */
public class TrackTagRelationship implements TrackTag, Comparable {
    Long id;

    private Tag tag;

    private Long primaryKey;

    private String tagType;

    private Integer weight;

    private String abAdded = "y"; // By default, all tag relationships are added via AB InputBeans
    private GeoData geoData;
    //  relationships added outside of AB will not have this property unless manually set

    protected TrackTagRelationship() {
    }

    /**
     * For non-persistent relationship. If caller is not tracking in the graph, then this
     * constructor can be used to create header data suitable for writing to search
     *
     * @param header       Header object
     * @param tag          Tag object
     * @param relationship Name of the relationship
     * @param propMap      Relationship properties
     */
    public TrackTagRelationship(MetaHeader header, Tag tag, String relationship, Map<String, Object> propMap) {
        this.primaryKey = header.getId();
        this.tag = tag;
        this.tagType = relationship;
        this.id = System.currentTimeMillis() + relationship.hashCode(); // random...
        if (propMap != null) {
            if (propMap.get("weight") != null)
                this.weight = (Integer) propMap.get("weight");
        }
    }

    public TrackTagRelationship(Long pk, Tag tag) {
        this(pk, tag, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackTagRelationship)) return false;

        TrackTagRelationship that = (TrackTagRelationship) o;

        if (primaryKey != null ? !primaryKey.equals(that.primaryKey) : that.primaryKey != null) return false;
//        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (tag != null ? !tag.equals(that.tag) : that.tag != null) return false;
        if (tagType != null ? !tagType.equals(that.tagType) : that.tagType != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return "TrackTagRelationship{" +
                "primaryKey=" + primaryKey +
                ", tag=" + tag +
                ", tagType='" + tagType + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        //int result = id != null ? id.hashCode() : 0;
        int result = (tag != null ? tag.hashCode() : 0);
        result = 31 * result + (primaryKey != null ? primaryKey.hashCode() : 0);
        result = 31 * result + (tagType != null ? tagType.hashCode() : 0);
        return result;
    }

    public TrackTagRelationship(Long primaryKey, Tag tag, Relationship relationship) {
        this();
        this.primaryKey = primaryKey;
        this.tag = tag;
        this.tagType = (relationship == null ? tag.getName() : relationship.getType().name());
        if ( relationship!= null ) {
            if (relationship.hasProperty("weight"))
                this.weight = (Integer) relationship.getProperty("weight");

            // Flags the relationship as having been created by a user rather than a system process
            if (relationship.hasProperty("abAdded"))
                this.abAdded = (String) relationship.getProperty("abAdded");
            this.id = relationship.getId();
        }

    }


    public Long getId() {
        return id;
    }

    @Override
    public Tag getTag() {
        return tag;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @JsonIgnore
    public Long getPrimaryKey() {
        return primaryKey;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @JsonIgnore
    public String getTagType() {
        return tagType;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getProperties() {
        return tag.getProperties();
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getWeight() {
        return weight;
    }

    @Override
    public int compareTo(Object o) {
        //ToDo: What?????
        return 1;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public void setGeoData(GeoData geoData) {
        this.geoData = geoData;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public GeoData getGeoData() {
        return geoData;
    }
}
