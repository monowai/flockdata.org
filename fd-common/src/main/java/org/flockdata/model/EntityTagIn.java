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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Map;

/**
 * "Reversed" relationship
 *
 * @author mholdsworth
 * @since 29/06/2013
 * @tag Relationship, EntityTag, Tag, Entity
 */
@RelationshipEntity (type = "ENTITY-TAG-IN")
public class EntityTagIn extends AbstractEntityTag {

    @GraphId
    Long id =null;

    @StartNode
    @Fetch
    Tag tag;

    @EndNode
    Entity entity;

    @RelationshipType
    @Fetch
    DynamicRelationshipType relationship;

    protected EntityTagIn() {
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
    public EntityTagIn(Entity entity, Tag tag, String relationship, Map<String, Object> propMap) {
        this();
        this.entity = entity;
        this.tag = tag;
        this.relationship= DynamicRelationshipType.withName(relationship);
        if ( propMap!=null && ! propMap.isEmpty())
            this.properties = new DynamicPropertiesContainer(propMap);
    }

    public EntityTagIn(Entity entity, EntityTag logTag) {
        this.entity = entity;
        this.tag = logTag.getTag();
        this.properties = new DynamicPropertiesContainer(logTag.getProperties());
        this.relationship = DynamicRelationshipType.withName(logTag.getRelationship());

    }

    @Override
    public Long getId (){
        return id;
    }

    @Override
    @JsonIgnore
    public Entity getEntity() {
        return entity;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getTagProperties() {
        return tag.getProperties();
    }

    @Override
    @JsonIgnore
    public Boolean isReversed() {
        return false;
    }

    public String getRelationship() {
        if ( relationship != null)
            return relationship.name();
        return "ENTITY-TAG-IN";
    }

    public void setRelationship(String relationship) {
        if (relationship != null)
            this.relationship = DynamicRelationshipType.withName(relationship);
    }

    @Override
    @JsonIgnore
    public boolean isGeo() {
        return geoRelationship;
    }

    @Override
    public String toString() {
        return "EntityTagIn{" +
                "id=" + id+
                ", tag=" + tag +
                ", entity='" + entity + '\'' +
                '}';
    }

}
