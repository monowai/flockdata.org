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

package org.flockdata.engine.schema.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.registration.model.Relationship;
import org.flockdata.track.model.DocumentType;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Collection;
import java.util.TreeSet;

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

    @RelatedTo(elementClass = DocumentTypeNode.class, type="DOC_RELATIONSHIP", direction = Direction.OUTGOING)
    Collection<DocumentType> documentTypes;

    RelationshipNode(){}

    public RelationshipNode(String relationship, DocumentType documentType) {
        this();
        setName(relationship);
        addDocumentType(documentType);
    }

    public void addDocumentType(DocumentType documentType) {
        if (documentTypes == null ){
            documentTypes = new TreeSet<>();
        }
        documentTypes.add(documentType);
    }

    @Override
    public String toString() {
        return "RelationshipNode{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean hasDocumentType(DocumentType document) {
        if ( documentTypes == null )
            return false;
        for (DocumentType documentType : documentTypes) {
            if ( documentType.getId().equals(document.getId()))
                return true;
        }
        return false;
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

    @JsonIgnore
    public Collection<DocumentType> getDocumentTypes(){
        return documentTypes;
    }
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
