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

import org.flockdata.company.model.FortressNode;
import org.flockdata.registration.model.Fortress;
import org.flockdata.track.model.Concept;
import org.flockdata.track.model.DocumentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.neo4j.graphdb.Direction;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Mike Holdsworth
 * Date: 30/06/13
 * Time: 10:02 AM
 */
@NodeEntity
@TypeAlias("DocType")
public class DocumentTypeNode implements DocumentType, Comparable<DocumentType>{

    @GraphId
    Long id;

    private String name;

    @Indexed
    private String code;

    @Indexed(unique = true)
    private String companyKey;

    @RelatedTo(elementClass = FortressNode.class, type = "FORTRESS_DOC", direction = Direction.OUTGOING)
    //@Fetch
    private FortressNode fortress;

    @RelatedTo(elementClass = ConceptNode.class,  type = "HAS_CONCEPT", direction = Direction.OUTGOING)
    Collection<Concept> concepts;

    protected DocumentTypeNode() {
    }

    public DocumentTypeNode(Fortress fortress, String documentType) {
        this();
        this.name = documentType;
        this.code = parse(fortress, documentType);

        if ( fortress !=null ){
            this.companyKey = fortress.getCompany().getId() + "." + code;
            addFortress(fortress);
        }

    }

    public DocumentTypeNode(DocumentType document) {
        this(document.getFortress(), document.getName());
        this.id = document.getId();
    }

    private void addFortress(Fortress fortress) {
        this.fortress = (FortressNode) fortress;
    }

    public String getName() {
        return name;
    }

    @Override
    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    @JsonIgnore
    public String getCode() {
        return code;
    }


    /**
     * used to create a unique key index for a company+docType combo
     */
    @JsonIgnore
    public String getCompanyKey() {
        return companyKey;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Collection<Concept> getConcepts() {
        return concepts;
    }

    @Override
    public Fortress getFortress() {
        return fortress;
    }

    @Override
    public void add(Concept concept) {
        if ( concepts == null )
            concepts = new ArrayList<>();
        concepts.add(concept);
    }

    @Override
    public String toString() {
        return "DocumentTypeNode{" +
                "id=" + id +
                ", fortress=" + fortress +
                ", name='" + name + '\'' +
                '}';
    }

    public static String parse(Fortress fortress, String documentType) {
//        return documentType.toLowerCase().replaceAll("\\s", ".");
        return fortress.getId() + "."+ documentType.toLowerCase().replaceAll("\\s", ".");
    }

    @Override
    public int compareTo(DocumentType o) {
        return o.getCompanyKey().compareTo(companyKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentTypeNode)) return false;

        DocumentTypeNode that = (DocumentTypeNode) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (companyKey != null ? !companyKey.equals(that.companyKey) : that.companyKey != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (companyKey != null ? companyKey.hashCode() : 0);
        return result;
    }
}
