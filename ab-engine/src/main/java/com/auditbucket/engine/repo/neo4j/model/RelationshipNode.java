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
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 2:33 PM
 */
@NodeEntity
@TypeAlias("Relationship")
public class RelationshipNode implements Relationship, Comparable<RelationshipNode> {
    @GraphId
    Long id;

//    @RelatedTo(type="KNOWN_RELATIONSHIP", direction = Direction.INCOMING)
//    ConceptNode concept;

    RelationshipNode(){}

    public RelationshipNode(String relationship) {
        this();
        setName(relationship);
    }

    public Long getId() {
        return id;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    @JsonIgnore
//    public ConceptNode getConcept() {
//        return concept;
//    }
//
//    public void setConcept(ConceptNode concept) {
//        this.concept = concept;
//    }

    private String name;

    @Override
    public int compareTo(RelationshipNode o) {
        if ( o== null || name == null )
            return -1;
        // ToDO: Review this implementation
        //int d =  (concept== null ?0:concept.getDocumentTypes().getCode().compareTo(o.getConcept().getDocumentTypes().getCode()));
        //if ( d==0)
            return name.compareTo(o.getName());
        //return d;

    }
}
