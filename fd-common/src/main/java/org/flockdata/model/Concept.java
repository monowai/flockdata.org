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

package org.flockdata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.flockdata.track.bean.ConceptInputBean;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * User: mike
 * Date: 16/06/14
 * Time: 10:16 AM
 */
@NodeEntity
@TypeAlias("Concept")
public class Concept {

    @GraphId
    Long id;

    private String name;

    @Indexed(unique = true)
    private String key;

    @RelatedTo(elementClass = Relationship.class, type = "KNOWN_TAG", direction = Direction.OUTGOING)
    @Fetch
    Set<Relationship> knownTags;

    @RelatedTo(elementClass = Relationship.class, type = "KNOWN_ENTITY", direction = Direction.OUTGOING)
    @Fetch
    Set<Relationship> knownEntities;

    private String type = "T";

    protected Concept() {
    }

    public Concept(String name) {
        this();
        this.name = name;

    }

    public Concept(ConceptInputBean concept, String relationship, DocumentType connectedTo) {
        this(concept.getName());
        if (concept.isTag()) {
            addTagRelationship(relationship, connectedTo);
        } else {
            this.type = "E";
            addEntityRelationship(relationship, connectedTo);
        }
        this.key = toKey(concept);
    }

    public static String toKey(ConceptInputBean concept) {
        String type = "T";
        if (!concept.isTag())
            type = "E";
        return type + "." + concept.getName().toLowerCase();
    }

    public void addTagRelationship(String relationship, DocumentType docType) {
        if (knownTags == null)
            knownTags = new HashSet<>();

        Relationship node = new Relationship(relationship, docType);
        knownTags.add(node);
    }

    /**
     * Entity to Entity relationship meta map.
     * <p/>
     * Creates a relationship between this concept and another connectedTo.
     *
     * @param relationship named rlx
     * @param docType
     */
    public void addEntityRelationship(String relationship, DocumentType docType) {
        if (knownEntities == null)
            knownEntities = new HashSet<>();

        Relationship node = new Relationship(relationship, docType);
        knownEntities.add(node);

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Collection<Relationship> getKnownTags() {
        return knownTags;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Collection<Relationship> getKnownEntities() {
        return knownEntities;
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
        return "DocumentType{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Concept)) return false;

        Concept that = (Concept) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public Relationship hasTagRelationship(String relationshipName, DocumentType docType) {
        return hasRelationship(knownTags, relationshipName, docType);
    }

    public Relationship hasEntityRelationship(String relationshipName, DocumentType docType) {
        return hasRelationship(knownEntities, relationshipName, docType);
    }

    private Relationship hasRelationship(Collection<Relationship> analyze, String relationshipName, DocumentType docType) {
        if (analyze == null)
            return null;
        for (Relationship relationship : analyze) {
            if (relationship.getName().equalsIgnoreCase(relationshipName)) {
                for (DocumentType documentType : relationship.getDocumentTypes()) {
                    if (documentType.getId().equals(docType.getId()))
                        return relationship;
                }
            }
        }
        return null;
    }


    /**
     * @return E for Entity, T for Tag
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}