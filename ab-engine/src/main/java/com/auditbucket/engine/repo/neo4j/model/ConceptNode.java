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

import com.auditbucket.registration.model.Relationship;
import com.auditbucket.track.model.Concept;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:02 AM
 */
@NodeEntity
@TypeAlias("Concept")
public class ConceptNode implements Concept {

    @GraphId
    Long id;

    @Indexed(unique = true)
    private String name;

    @RelatedTo(elementClass = RelationshipNode.class, type="KNOWN_RELATIONSHIP", direction = Direction.OUTGOING)
    Set<Relationship> relationships;

    protected ConceptNode() {
    }

    public ConceptNode(String indexName) {
        this();
        this.name = indexName;

    }

    public ConceptNode(String indexName, String relationship) {
        this(indexName);
        addRelationship(relationship);

    }

    public void addRelationship(String relationship) {
        if ( relationships == null )
            relationships = new TreeSet<>();

        RelationshipNode node = new RelationshipNode(relationship);
        relationships.add(node);
    }

    public Collection<Relationship> getRelationships() {
        return relationships;
    }


    public String getName() {
        return name;
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "DocumentTypeNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    public Relationship hasRelationship(String relationshipName) {
        if ( relationships == null )
            return null;
        for (Relationship relationship : relationships) {
            if (relationship.getName().equalsIgnoreCase(relationshipName))
                return relationship;
        }
        return null;
    }
}
