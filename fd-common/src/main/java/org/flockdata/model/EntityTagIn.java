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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Map;

/**
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:59 PM
 */
@RelationshipEntity (type = "ENTITY-TAG-IN")
public class EntityTagIn extends EntityTag {

    @GraphId
    Long id =null;

    @StartNode
    @Fetch
    Tag tag;

    @EndNode
    Entity entity;

    @RelationshipType
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
    public Boolean isReversed() {
        return false;
    }

    public String getRelationship() {
        if ( relationship != null)
            return relationship.name();
        return "ENTITY-TAG-IN";
    }

    public void setRelationship(String relationship){
        if ( relationship != null )
            this.relationship = DynamicRelationshipType.withName(relationship);
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
