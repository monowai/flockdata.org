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
import java.util.Collection;
import java.util.HashSet;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * Meta data - stores a representation of a Relationship
 *
 * @author mholdsworth
 * @tag Schema, Entity
 * @since 16/06/2014
 */
@NodeEntity
@TypeAlias("Relationship")
public class RelationshipNode implements Comparable<RelationshipNode> {
  @GraphId
  Long id;

  @RelatedTo(elementClass = DocumentNode.class, type = "DOC_RELATIONSHIP", direction = Direction.OUTGOING)
  Collection<DocumentNode> documentTypes;
  private String name;

  RelationshipNode() {
  }

  public RelationshipNode(String relationship, DocumentNode documentType) {
    this();
    setName(relationship);
    addDocumentType(documentType);
  }

  public void addDocumentType(DocumentNode documentType) {
    if (documentTypes == null) {
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

  public boolean hasDocumentType(DocumentNode document) {
    if (documentTypes == null) {
      return false;
    }
    for (DocumentNode documentType : documentTypes) {
      if (documentType.getId().equals(document.getId())) {
        return true;
      }
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
  public Collection<DocumentNode> getDocumentTypes() {
    return documentTypes;
  }

  @Override
  public int compareTo(RelationshipNode o) {
    if (o == null || name == null) {
      return -1;
    }
    // ToDO: Review this implementation
    //int d =  (concept== null ?0:concept.getDocumentTypes().getCode().compareTo(o.getConcept().getDocumentTypes().getCode()));
    //if ( d==0)
    return name.compareTo(o.getName());
    //return d;

  }
}
