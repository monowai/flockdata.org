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

package org.flockdata.engine.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.data.AbstractEntityTag;
import org.flockdata.data.Entity;
import org.flockdata.data.EntityTag;
import org.flockdata.data.Tag;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.util.Map;

/**
 * Regular non-reversed relationship between an entity and a tag
 *
 * @author mholdsworth
 * @since 29/06/2013
 * @tag Relationship, EntityTag, Tag, Entity
 */
@RelationshipEntity  (type = "ENTITY-TAG-OUT")
public class EntityTagOutRlx extends AbstractEntityTag {

    @StartNode protected EntityNode entity;
    @EndNode
    @Fetch protected TagNode tag;
    protected DynamicProperties properties = new DynamicPropertiesContainer();
    @GraphId
    Long id;
    @RelationshipType
    @Fetch
    private DynamicRelationshipType relationship ;

    protected EntityTagOutRlx() {

    }

    public EntityTagOutRlx(EntityNode entity, TagNode tag) {
        this(entity, tag,  "ENTITY-TAG-OUT", null);
    }

    /**
     * For non-persistent relationship. If caller is not tracking in the graph, then this
     * constructor can be used to create entity data suitable for writing to search
     * @param entity       Entity object
     * @param tag          Tag object
     * @param relationship Name of the relationship
     * @param propMap      Relationship properties
     */
    public EntityTagOutRlx(Entity entity, Tag tag, String relationship, Map<String, Object> propMap) {
        this();
        this.entity = (EntityNode)entity;
        this.tag = (TagNode)tag;
        this.relationship = DynamicRelationshipType.withName(relationship);
        if (propMap != null && !propMap.isEmpty())
            this.properties = new DynamicPropertiesContainer(propMap);

    }

    public EntityTagOutRlx(EntityNode entity, EntityTag logTag) {
        this.entity = entity;
        this.tag = (TagNode)logTag.getTag();
        this.properties = new DynamicPropertiesContainer(logTag.getProperties());
        this.relationship = DynamicRelationshipType.withName(logTag.getRelationship());

    }

    @Override
    public Long getId() {
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

    public String getRelationship() {
        if ( relationship != null)
            return relationship.name();
        return "ENTITY-TAG-OUT";
    }

    public void setRelationship(String relationship){
        if ( relationship != null )
            this.relationship = DynamicRelationshipType.withName(relationship);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getTagProperties() {
        return tag.getProperties();
    }

    @Override
    @JsonIgnore
    public boolean isGeoRelationship() {
        return geoRelationship;
    }

    public Object getProperty(String key) {
        if (properties == null)
            return null;
        return properties.getProperty(key);
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties.asMap();
    }

    @Override
    @JsonIgnore
    public Boolean isReversed() {
        return true;
    }

    @Override
    public String toString() {
        return "EntityTagOut {" +
                "id=" + id +
                ", tag=" + tag +
                ", entity='" + entity + '\'' +
                '}';
    }

    public int compareTo(AbstractEntityTag o) {
        int val = getRelationship().compareTo(o.getRelationship());
        if (val == 0)
            return getTag().getCode().compareTo(o.getTag().getCode());
        return val;

    }

}
