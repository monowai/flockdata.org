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
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Collection;
import java.util.HashSet;

/**
 * User: mike
 * Date: 16/06/14
 * Time: 4:00 PM
 */
@NodeEntity
@TypeAlias("Relationship")
public class Relationship implements Comparable<Relationship> {
    @GraphId
    Long id;

    @RelatedTo(elementClass = DocumentType.class, type="DOC_RELATIONSHIP", direction = Direction.OUTGOING)
    Collection<DocumentType> documentTypes;

    Relationship(){}

    public Relationship(String relationship, DocumentType documentType) {
        this();
        setName(relationship);
        addDocumentType(documentType);
    }

    public void addDocumentType(DocumentType documentType) {
        if (documentTypes == null ){
            documentTypes = new HashSet<>();
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
    public int compareTo(Relationship o) {
        if ( o== null || name == null )
            return -1;
        // ToDO: Review this implementation
        //int d =  (concept== null ?0:concept.getDocumentTypes().getCode().compareTo(o.getConcept().getDocumentTypes().getCode()));
        //if ( d==0)
        return name.compareTo(o.getName());
        //return d;

    }
}
