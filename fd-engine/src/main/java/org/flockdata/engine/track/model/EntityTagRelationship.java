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

package org.flockdata.engine.track.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.registration.model.Tag;
import org.flockdata.track.model.Entity;
import org.flockdata.track.model.EntityTag;
import org.flockdata.track.model.GeoData;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:59 PM
 */
public class EntityTagRelationship implements EntityTag, Comparable<EntityTag> {
    Long id;

    private Tag tag;
    private Long primaryKey;
    private String relationship;
    private Map<String, Object> properties = new HashMap<>();
    //private Map<String, GeoData>geoData = new HashMap<>();
    private GeoData geoData;

    protected EntityTagRelationship() {
    }

    /**
     * For non-persistent relationship. If caller is not tracking in the graph, then this
     * constructor can be used to create entity data suitable for writing to search
     *
     * @param entity       Entity object
     * @param tag          Tag object
     * @param relationship Name of the relationship
     * @param propMap      Relationship properties
     */
    public EntityTagRelationship(Entity entity, Tag tag, String relationship, Map<String, Object> propMap) {
        this();
        this.primaryKey = entity.getId();
        this.tag = tag;
        this.relationship = relationship;
        this.id = System.currentTimeMillis() + relationship.hashCode(); // random...
        this.properties = propMap;
    }

    public EntityTagRelationship(Long pk, Tag tag) {
        this(pk, tag, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityTagRelationship)) return false;

        EntityTagRelationship that = (EntityTagRelationship) o;

        if (primaryKey != null ? !primaryKey.equals(that.primaryKey) : that.primaryKey != null) return false;
        if (tag != null ? !tag.equals(that.tag) : that.tag != null) return false;
        if (relationship != null ? !relationship.equals(that.relationship) : that.relationship != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return "EntityTagRelationship{" +
                "primaryKey=" + primaryKey +
                ", tag=" + tag +
                ", relationship='" + relationship + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        int result = (tag != null ? tag.hashCode() : 0);
        result = 31 * result + (primaryKey != null ? primaryKey.hashCode() : 0);
        result = 31 * result + (relationship != null ? relationship.hashCode() : 0);
        return result;
    }

    public EntityTagRelationship(Long primaryKey, Tag tag, Relationship relationship) {
        this();
        this.primaryKey = primaryKey;
        this.tag = tag;
        this.relationship = (relationship == null ? tag.getName() : relationship.getType().name());
        if ( relationship!= null ) {

            for (String rlxKey : relationship.getPropertyKeys()) {
                addProperty(rlxKey, relationship.getProperty(rlxKey));
            }

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
    public String getRelationship() {
        return relationship;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getTagProperties() {
        return tag.getProperties();
    }

    @Override
    public Long getWeight() {
        return (Long)getProperty("weight");
    }

    private void addProperty(String key, Object value){
        if ( key == null )
            return;

        if (properties == null )
            properties = new HashMap<>();
        properties.put(key, value);
    }

    private Object getProperty(String key){
        if (properties == null)
            return null;
        return properties.get(key);
    }

    public EntityTag setGeoData(GeoData geoData) {
        this.geoData = geoData;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public GeoData getGeoData() {
        return geoData;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public int compareTo(EntityTag o) {
        int val = getRelationship().compareTo(o.getRelationship());
        if ( val == 0 )
            return getTag().getCode().compareTo(o.getTag().getCode());
        return val;

    }
}
