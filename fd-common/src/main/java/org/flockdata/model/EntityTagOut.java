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
 * "Normal" non-reversed relationship entity->tag
 *
 * User: Mike Holdsworth
 * Date: 29/06/13
 * Time: 12:59 PM
 */
@RelationshipEntity  (type = "ENTITY-TAG-OUT")
public class EntityTagOut extends AbstractEntityTag {

    @GraphId
    Long id;

    @StartNode protected Entity entity;

    @EndNode
    @Fetch protected Tag tag;

    @RelationshipType
    @Fetch
    private DynamicRelationshipType relationship ;

    protected EntityTagOut() {

    }

    public EntityTagOut(Entity entity, Tag tag) {
        this(entity, tag,  "ENTITY-TAG-OUT", null);
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
    public EntityTagOut(Entity entity, Tag tag, String relationship, Map<String, Object> propMap) {
        this();
        this.entity = entity;
        this.tag = tag;
        this.relationship = DynamicRelationshipType.withName(relationship);
        if (propMap != null && !propMap.isEmpty())
            this.properties = new DynamicPropertiesContainer(propMap);

    }

    public EntityTagOut(Entity entity, EntityTag logTag) {
        this.entity = entity;
        this.tag = logTag.getTag();
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
    public boolean isGeo() {
        return geoRelationship;
    }

    @Override
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
