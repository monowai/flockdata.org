/*
 * Copyright (c) 2012-2015 "FlockData LLC"
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

package org.flockdata.neo4j;

import org.flockdata.model.Entity;
import org.flockdata.model.EntityTag;
import org.flockdata.model.Tag;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mholdsworth
 * @since 14/07/2015
 */
public class EntityTagNode extends EntityTag {
    Long id;
    Entity entity;
    Tag tag;
    String relationship;
    Boolean reversed;
    Map<String,Object>properties = new HashMap<>();

    public EntityTagNode(Relationship relationship, Node tag) {
        this.id = relationship.getId();
        this.relationship = relationship.getType().name();
        this.tag = new Tag(tag);
        reversed = relationship.getEndNode().getId() != tag.getId();
        for (String s : relationship.getPropertyKeys()) {
            properties.put(s, relationship.getProperty(s));
        }
    }

    public EntityTagNode(Entity e, Relationship relationship, Node tag) {
        this( relationship, tag);
        this.entity = e;
    }

    public EntityTagNode(Node entity, Node tag, Relationship r) {
        this ( new Entity(entity), r, tag)  ;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @Override
    public String getRelationship() {
        return relationship;
    }

    @Override
    public Map<String, Object> getTagProperties() {
        return null;
    }

    @Override
    public Boolean isReversed() {
        return reversed;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void setRelationship(String name) {
        this.relationship = name;
    }
}
